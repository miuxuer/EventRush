package com.miuxuer.eventrush.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * 全局唯一 ID 生成器，用于生成抢票订单号等主键。
 * <p>
 * 生成的 ID 是一个 64 位的 long：高 32 位存"当前时间戳 - 起始时间戳"（秒），低 32 位存当天的自增序列。
 * 序列号借助 Redis 的 INCR 保证并发下不重复，因此同一时刻多台机器也能生成出不同的 ID，
 * 并且 ID 整体单调递增、按日期可读。
 * </p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
@Component
public class RedisIdWorker {
    /**
     * 开始时间戳（2022-01-01 00:00:00 UTC 对应的秒数）
     */
    private static final long BEGIN_TIMESTAMP = 1640995200L;

    /**
     * 序列号的位数
     * 通常分配 32 位给序列号
     */
    private static final long COUNT_BITS = 32L;

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 构造方法注入 StringRedisTemplate。
     *
     * @param stringRedisTemplate 用于自增序列号的 Redis 客户端
     */
    public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 生成全局唯一 ID
     * @param keyPrefix 业务前缀（如 "order", "event" 等）
     * @return 全局唯一 ID（long 类型）
     */
    public long nextId(String keyPrefix) {
        // 1. 生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowSecond - BEGIN_TIMESTAMP;

        // 2. 生成序列号
        // 2.1. 获取当前日期，精确到天（方便按天做 Redis key 的统计和过期清理）
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        // 2.2. 自增长（利用 Redis 的 incr 命令保证原子性）
        long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);

        // 3. 拼接并返回
        // 时间戳向左移 32 位，空出低 32 位，然后与序列号进行按位或运算
        return timestamp << COUNT_BITS | count;
    }

}
