package com.miuxuer.eventrush.utils;

/**
 * Redis 中各类 key 的前缀与过期时间常量。
 *
 * <p>统一在这里维护，避免 key 拼接散落在各个业务类里对不上。</p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
public class RedisConstants {

    /** 短信验证码的 key 前缀，完整 key 形如 login:code:13800000000 */
    public static final String LOGIN_CODE_KEY = "login:code:";
    /** 验证码有效期，单位分钟 */
    public static final Long LOGIN_CODE_TTL = 2L;

    /** 登录态 token 的 key 前缀，完整 key 形如 login:token:xxxx */
    public static final String LOGIN_USER_KEY = "login:token:";
    /** 登录态有效期，单位分钟 */
    public static final Long LOGIN_USER_TTL = 36000L;

    /** 缓存空值时的较短过期时间，单位分钟，用来防止缓存穿透 */
    public static final Long CACHE_NULL_TTL = 2L;

    /** 演出详情缓存的有效期，单位分钟 */
    public static final Long CACHE_EVENT_TTL = 30L;
    /** 演出详情缓存的 key 前缀 */
    public static final String CACHE_EVENT_KEY = "cache:event:";

    /** 重建演出缓存时用的互斥锁 key 前缀 */
    public static final String LOCK_EVENT_KEY = "lock:event:";
    /** 重建缓存锁的持有时间，单位秒 */
    public static final Long LOCK_EVENT_TTL = 10L;

    /** 票档余票的 key 前缀，完整 key 形如 ticket:stock:1，由抢票 Lua 脚本读写 */
    public static final String TICKET_STOCK_KEY = "ticket:stock:";

    /** 某个票档已抢到票的用户集合 key 前缀，完整 key 形如 ticket:order:1，用于保证一人一单 */
    public static final String TICKET_ORDER_KEY = "ticket:order:";

    /** 观演笔记点赞用户的 ZSet key 前缀 */
    public static final String POST_LIKED_KEY = "post:liked:";

    /** 关注流（收件箱）的 ZSet key 前缀，完整 key 形如 feed:12 */
    public static final String FEED_KEY = "feed:";

    /** 按分类存放演出坐标的 GEO key 前缀，完整 key 形如 event:geo:1 */
    public static final String EVENT_GEO_KEY = "event:geo:";

    /** 用户签到记录的 BitMap key 前缀，完整 key 形如 sign:12:202610 */
    public static final String USER_SIGN_KEY = "sign:";
}
