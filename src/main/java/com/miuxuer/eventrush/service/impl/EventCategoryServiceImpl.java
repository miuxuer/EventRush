package com.miuxuer.eventrush.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.miuxuer.eventrush.entity.EventCategory;
import com.miuxuer.eventrush.mapper.EventCategoryMapper;
import com.miuxuer.eventrush.service.IEventCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.miuxuer.eventrush.utils.RedisConstants.CACHE_EVENT_TTL;

/**
 * 演出分类服务实现。
 *
 * <p>分类列表整体只有十几条，而且几乎不变，所以直接把整个列表序列化成一个 key 缓存起来，
 * 不做单条缓存。</p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
@Service
@Slf4j
public class EventCategoryServiceImpl extends ServiceImpl<EventCategoryMapper, EventCategory> implements IEventCategoryService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 查询全部演出分类，按 sort 字段升序。
     *
     * <p>先读缓存，缓存没有就查库并回写，缓存时间和其他演出缓存保持一致。</p>
     *
     * @return 分类列表，没有分类时返回空列表
     */
    @Override
    public List<EventCategory> queryCategoryList() {
        // 先查 Redis 缓存，整个列表存成一个 JSON 字符串
        String key = "cache:event:category";
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(json)) {
            return JSONUtil.toList(json, EventCategory.class);
        }

        // 缓存没有，查数据库
        List<EventCategory> categoryList = query().orderByAsc("sort").list();
        if (categoryList == null || categoryList.isEmpty()) {
            return Collections.emptyList();
        }
        // 回写缓存
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(categoryList), CACHE_EVENT_TTL, TimeUnit.MINUTES);

        return categoryList;
    }
}
