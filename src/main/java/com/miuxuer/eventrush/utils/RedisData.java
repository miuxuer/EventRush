package com.miuxuer.eventrush.utils;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 缓存"逻辑过期"包装类，把业务数据与它的逻辑过期时间一起写入 Redis。
 * <p>
 * 与给 key 设置物理 TTL 不同，这里的 expireTime 只是数据上的标记：
 * 读到数据后由调用方判断是否已过期，过期则异步重建缓存，从而避免缓存击穿。
 * </p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
@Data
public class RedisData {

    /**
     * 逻辑过期时间，到达该时间即认为缓存过期，需要异步刷新
     */
    private LocalDateTime expireTime;

    /**
     * 真正的业务数据
     */
    private Object data;
}
