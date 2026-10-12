package com.miuxuer.eventrush.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.miuxuer.eventrush.dto.Result;
import com.miuxuer.eventrush.entity.Event;
import com.miuxuer.eventrush.mapper.EventMapper;
import com.miuxuer.eventrush.service.IEventService;
import com.miuxuer.eventrush.utils.RedisData;
import com.miuxuer.eventrush.utils.SystemConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.miuxuer.eventrush.utils.RedisConstants.*;

/**
 * 演出服务实现。
 *
 * <p>这个类里同时放了三种缓存方案，实际生效的是「逻辑过期」那种：</p>
 * <ul>
 *   <li>{@link #queryWithPassThrough} —— 缓存穿透方案：查不到就写个空值进 Redis 挡住后续请求；</li>
 *   <li>{@link #queryWithMutex} —— 缓存击穿方案一：互斥锁，只放一个线程去查库重建缓存；</li>
 *   <li>{@link #queryWithLogicalExpire} —— 缓存击穿方案二：缓存里存逻辑过期时间，过期后由一个后台线程异步重建，
 *       期间所有请求都先返回旧数据，所以不会阻塞。</li>
 * </ul>
 *
 * <p>另外演出列表还支持按距离排序，靠的是 Redis 的 GEO 数据结构。</p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
@Service
public class EventServiceImpl extends ServiceImpl<EventMapper, Event> implements IEventService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /** 重建缓存用的线程池，独立于请求线程，避免拖慢接口响应 */
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    /**
     * 根据 id 查询演出详情。
     *
     * @param id 演出id
     * @return 演出详情；演出不存在时返回失败提示
     */
    @Override
    public Result queryById(Long id) {
        // 三种方案在这里切换，当前启用的是「逻辑过期」，兼顾了高并发下不阻塞
        //Event event = queryWithPassThrough(id);
        //Event event = queryWithMutex(id);
        Event event = queryWithLogicalExpire(id);

        if (event == null) {
            return Result.fail("演出不存在");
        }
        return Result.ok(event);
    }

    /**
     * 【缓存击穿方案二】逻辑过期。
     *
     * <p>缓存永远不设物理过期时间，而是在 value 里塞一个逻辑过期时间。请求进来先看逻辑时间：</p>
     * <ul>
     *   <li>没过期 —— 直接返回缓存，全程不碰数据库；</li>
     *   <li>过期了 —— 返回旧数据的同时，抢到互斥锁的那个线程起一个后台线程去重建缓存。</li>
     * </ul>
     *
     * <p>相比互斥锁方案，这里所有请求都能立刻拿到数据，代价是短时间内数据可能偏旧。</p>
     *
     * @param id 演出id
     * @return 缓存的演出数据；缓存里没有这条记录时返回 null（说明是冷门演出，需要预热）
     */
    public Event queryWithLogicalExpire(Long id) {
        String key = CACHE_EVENT_KEY + id;
        // 从redis里查询演出缓存
        String cache = stringRedisTemplate.opsForValue().get(key);

        // 缓存里没有，说明这条演出没预热过，直接返回 null
        if (StrUtil.isBlank(cache)) {
            return null;
        }

        // 命中，把 json 反序列化为对象
        RedisData redisData = JSONUtil.toBean(cache, RedisData.class);
        Event event = JSONUtil.toBean((JSONObject) redisData.getData(), Event.class);
        LocalDateTime expireTime = redisData.getExpireTime();

        // 逻辑时间还没到，直接返回缓存数据
        if (expireTime.isAfter(LocalDateTime.now())) {
            return event;
        }

        // 已经逻辑过期，需要重建缓存
        String lockKey = LOCK_EVENT_KEY + id;
        boolean isLock = tryLock(lockKey);
        if (isLock) {
            // 抢到锁，交给后台线程去重建，当前请求不等它，仍然返回旧数据
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    saveEvent2Redis(id, 20L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    // 释放锁
                    unLock(lockKey);
                }
            });
        }

        // 没抢到锁的线程也直接返回旧数据，不会被阻塞
        return event;
    }

    /**
     * 【缓存击穿方案一】互斥锁。
     *
     * <p>缓存失效时，同一时刻只允许一个线程去查库重建缓存，其它线程休眠 50ms 后重试。
     * 好处是数据一致性强，坏处是没抢到锁的请求要在那里空转等待，高并发下响应会变慢。</p>
     *
     * @param id 演出id
     * @return 演出数据；演出不存在时返回 null
     */
    public Event queryWithMutex(Long id) {
        String key = CACHE_EVENT_KEY + id;
        // 从redis里查询演出缓存
        String cache = stringRedisTemplate.opsForValue().get(key);

        // 命中缓存，直接返回
        if (StrUtil.isNotBlank(cache)) {
            return JSONUtil.toBean(cache, Event.class);
        }

        // 命中的是一个空值，说明之前查过这个 id，库里确实没有，直接返回，挡住穿透
        if ("".equals(cache)) {
            return null;
        }

        String lockKey = LOCK_EVENT_KEY + id;
        Event event = null;

        // 注意：抢锁这一步必须放在 try 外面。
        // 因为 return 也会触发 finally，如果"没抢到锁"的分支写在 try 里，
        // 它 return 时会把别人正持有的锁删掉，互斥就失效了。
        boolean result = tryLock(lockKey);
        if (!result) {
            // 没抢到锁说明别人正在重建，歇一会儿再来
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return queryWithMutex(id);
        }

        // 走到这里说明锁已经在自己手里了，finally 里删的一定是自己的锁
        try {
            // 拿到锁，根据id查询数据库
            event = getById(id);
            if (event == null) {
                // 数据库也没有，把空值写进缓存，防止后面的请求继续打到数据库
                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            // 查到数据，写回缓存
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(event), CACHE_EVENT_TTL, TimeUnit.MINUTES);
        } finally {
            // 释放互斥锁
            unLock(lockKey);
        }

        return event;
    }

    /**
     * 【缓存穿透方案】缓存空值。
     *
     * <p>查库为空时也往 Redis 写一个空字符串并设一个较短的有效期（{@link RedisConstants#CACHE_NULL_TTL} 分钟），
     * 这样针对同一个不存在的 id 的大量请求都会被缓存挡住，不会反复打到数据库。</p>
     *
     * @param id 演出id
     * @return 演出数据；演出不存在时返回 null
     */
    public Event queryWithPassThrough(Long id) {
        String key = CACHE_EVENT_KEY + id;
        // 从redis里查询演出缓存
        String cache = stringRedisTemplate.opsForValue().get(key);

        // 命中缓存，直接返回
        if (StrUtil.isNotBlank(cache)) {
            return JSONUtil.toBean(cache, Event.class);
        }

        // 命中的是空值，说明库里确实没有，直接返回
        if ("".equals(cache)) {
            return null;
        }

        // 缓存里没有，去数据库查
        Event event = getById(id);
        if (event == null) {
            // 库中也没有，写空值进缓存挡住后续请求
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        // 查到数据，写回缓存
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(event), CACHE_EVENT_TTL, TimeUnit.MINUTES);
        return event;
    }

    /**
     * 把演出数据预热（或重建）到 Redis，value 是带逻辑过期时间的 {@link RedisData}。
     *
     * <p>方法里的 {@code Thread.sleep(200)} 是故意留的，用来模拟重建缓存比较慢的场景，
     * 方便观察逻辑过期方案下请求不会被阻塞。</p>
     *
     * @param id            演出id
     * @param expireSeconds 逻辑过期时间，单位秒，从当前时刻往后算
     * @throws InterruptedException 模拟耗时被中断时抛出
     */
    public void saveEvent2Redis(Long id, Long expireSeconds) throws InterruptedException {
        // 查询演出信息
        Event event = getById(id);
        // 模拟重建缓存的耗时
        Thread.sleep(200);

        RedisData redisData = new RedisData();
        redisData.setData(event);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        // 写入redis，注意这里不设物理过期时间，过期完全靠逻辑时间判断
        stringRedisTemplate.opsForValue().set(CACHE_EVENT_KEY + id, JSONUtil.toJsonStr(redisData));
    }

    /**
     * 获取互斥锁（重建缓存用）。
     *
     * @param key 锁的 key
     * @return true 表示抢到锁
     */
    private boolean tryLock(String key) {
        Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 5, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(result);
    }

    /**
     * 释放互斥锁。
     *
     * @param key 锁的 key
     */
    private void unLock(String key) {
        stringRedisTemplate.delete(key);
    }

    /**
     * 修改演出信息。
     *
     * <p>用的是「先更新数据库，再删除缓存」的策略。事务保证数据库更新失败时不会白删缓存。</p>
     *
     * @param event 演出信息，id 必填
     * @return 成功返回 ok；id 为空返回失败提示
     */
    @Override
    @Transactional
    public Result update(Event event) {
        Long id = event.getId();
        if (id == null) {
            return Result.fail("演出id不能为空");
        }
        // 更新数据库
        updateById(event);
        // 删除缓存，下次查询会重新加载
        stringRedisTemplate.delete(CACHE_EVENT_KEY + id);
        return Result.ok();
    }

    /**
     * 按分类分页查询演出列表，可选按距离排序。
     *
     * <p>传了经纬度就走 Redis GEO：先按坐标搜附近 5000 米内的演出并按距离排好序，
     * 再做内存分页（GEO 本身不支持物理分页），最后回数据库补全演出信息。
     * 不传经纬度就退化成普通的分页查询。</p>
     *
     * @param categoryId 演出分类id
     * @param current    页码，从 1 开始
     * @param x          当前所在位置的经度，可为 null
     * @param y          当前所在位置的纬度，可为 null
     * @return 演出列表，每条数据上带了 distance 字段（单位米）
     */
    @Override
    public Result queryEventByCategory(Integer categoryId, Integer current, Double x, Double y) {
        // 1. 没有坐标，直接按数据库普通分页查询
        if (x == null || y == null) {
            Page<Event> page = query()
                    .eq("category_id", categoryId)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            return Result.ok(page.getRecords());
        }

        // 2. 计算分页参数 (Redis Geo本身不支持物理分页，只能查出来后内存截取)
        int from = (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE;
        int end = current * SystemConstants.DEFAULT_PAGE_SIZE;

        // 3. 查询 Redis，按照距离排序、分页。结果包含：eventId、distance
        String key = EVENT_GEO_KEY + categoryId;
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo()
                .search(
                        key,
                        GeoReference.fromCoordinate(x, y),
                        new Distance(5000), // 查询半径5000米
                        RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end) // 带上距离，并限制返回条数
                );

        // 4. 解析出id
        if (results == null) {
            return Result.ok(Collections.emptyList());
        }
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list = results.getContent();

        if (list.size() <= from) {
            // 没有下一页了，结束
            return Result.ok(Collections.emptyList());
        }

        // 4.1 截取 from ~ end 的部分 (内存分页)
        List<Long> ids = new ArrayList<>(list.size());
        Map<String, Distance> distanceMap = new HashMap<>(list.size());

        list.stream().skip(from).forEach(result -> {
            // 4.2 获取演出id
            String eventIdStr = result.getContent().getName();
            ids.add(Long.valueOf(eventIdStr));
            // 4.3 获取距离
            Distance distance = result.getDistance();
            distanceMap.put(eventIdStr, distance);
        });

        // 5. 根据id查询演出详情
        String idStr = StrUtil.join(",", ids);
        // 这里的 last 是为了保证数据库查出来的顺序和 Redis 查出来的顺序一致
        List<Event> events = query().in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list();

        for (Event event : events) {
            // 把 Redis 里查出的距离塞进实体返回给前端
            event.setDistance(distanceMap.get(event.getId().toString()).getValue());
        }

        // 6. 返回
        return Result.ok(events);
    }
}
