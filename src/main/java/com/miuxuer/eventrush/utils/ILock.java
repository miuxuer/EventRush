package com.miuxuer.eventrush.utils;

/**
 * 分布式锁接口，抽象出"加锁 / 释放锁"两个动作。
 * <p>
 * 目前基于 Redis 的实现见 {@link SimpleRedisLock}，一个业务里拿到锁后必须在 finally 中调用 unlock，
 * 并且要用同一个线程/实例去解锁（实现类会把当前线程标识写进锁的 value，防止误删别人的锁）。
 * </p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
public interface ILock {

    /**
     * 尝试获取锁
     * @param timeoutSec 锁持有的超时时间，过期后自动释放
     * @return true代表获取锁成功；false代表获取锁失败
     */
    boolean tryLock(long timeoutSec);

    /**
     * 释放锁
     */
    void unlock();
}
