package com.miuxuer.eventrush;

import com.miuxuer.eventrush.entity.Event;
import com.miuxuer.eventrush.service.impl.EventServiceImpl;
import com.miuxuer.eventrush.utils.RedisIdWorker;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 基础功能测试：缓存预热、全局唯一 id 生成、演出坐标导入 Redis。
 *
 * <p>运行前需要先启动 MySQL 和 Redis，并保证 eventrush 库已经建好、数据已经导入。</p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
@SpringBootTest
class EventRushApplicationTests {

    @Resource
    private EventServiceImpl eventService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedisIdWorker redisIdWorker;

    private final ExecutorService es = Executors.newFixedThreadPool(500);

    /**
     * 把 id=1 的演出预热进 Redis 缓存，逻辑过期时间设成 20 秒。
     *
     * <p>之后调用查询接口时，20 秒内直接命中缓存；超过 20 秒才会触发后台线程重建。</p>
     *
     * @throws InterruptedException 预热方法里模拟了耗时，被中断时抛出
     */
    @Test
    void testSaveEventCache() throws InterruptedException {
        eventService.saveEvent2Redis(1L, 20L);
    }

    /**
     * 压测全局唯一 id 生成器：500 个线程各生成 100 个 id，共 5 万个，观察耗时。
     *
     * <p>用来验证「时间戳左移 32 位 + Redis 自增」这套方案在并发下不会产生重复 id，
     * 也不会成为瓶颈。</p>
     *
     * @throws InterruptedException 等待所有线程结束时被中断
     */
    @Test
    void testIdWorker() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(300);

        Runnable task = () -> {
            for (int i = 0; i < 100; i++) {
                long id = redisIdWorker.nextId("order");
                System.out.println("id = " + id);
            }
            latch.countDown();
        };

        long begin = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            es.submit(task);
        }
        latch.await();
        long end = System.currentTimeMillis();
        System.out.println("time = " + (end - begin));
    }

    /**
     * 把所有演出的经纬度按分类导入 Redis 的 GEO，供「按距离查演出」使用。
     *
     * <p>一次性把全部数据批量写入，比逐条 GEOADD 快得多，所以这里先收集成 List 再统一 add。</p>
     */
    @Test
    void loadEventGeoData() {
        // 1. 查询全部演出
        List<Event> list = eventService.list();
        // 2. 按分类分组，同一分类的演出写进同一个 GEO key
        Map<Long, List<Event>> map = list.stream().collect(Collectors.groupingBy(Event::getCategoryId));

        for (Map.Entry<Long, List<Event>> entry : map.entrySet()) {
            // 3.1. 分类id，也就是 GEO key 的后缀
            Long categoryId = entry.getKey();
            String key = "event:geo:" + categoryId;

            // 3.2. 组装这个分类下所有演出的坐标
            List<Event> events = entry.getValue();
            List<RedisGeoCommands.GeoLocation<String>> locations = new ArrayList<>(events.size());
            for (Event event : events) {
                // GEOADD key 经度 纬度 member，member 用演出 id
                locations.add(new RedisGeoCommands.GeoLocation<>(
                        event.getId().toString(),
                        new Point(event.getX(), event.getY())
                ));
            }

            // 3.3. 批量写入
            stringRedisTemplate.opsForGeo().add(key, locations);
        }
    }
}
