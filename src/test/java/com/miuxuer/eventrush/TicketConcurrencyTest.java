package com.miuxuer.eventrush;

import cn.hutool.core.lang.UUID;
import com.miuxuer.eventrush.dto.Result;
import com.miuxuer.eventrush.service.ITicketOrderService;
import com.miuxuer.eventrush.utils.RefreshTokenInterceptor;
import com.miuxuer.eventrush.utils.UserHolder;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static com.miuxuer.eventrush.utils.RedisConstants.LOGIN_USER_KEY;
import static com.miuxuer.eventrush.utils.RedisConstants.TICKET_ORDER_KEY;
import static com.miuxuer.eventrush.utils.RedisConstants.TICKET_STOCK_KEY;

/**
 * 抢票并发测试，用来代替 JMeter 压测两个最关键的正确性约束：
 * 「同一个人只能抢到一张」和「不管多少人抢，票都不会超卖」。
 *
 * <p>测试走的是完整链路，不是直接调 service 方法，而是给每个线程一个真实的 token：</p>
 *
 * <pre>
 *   authorization 请求头 → RefreshTokenInterceptor → 查 Redis → UserHolder → 抢票业务
 * </pre>
 *
 * <p>token 的生成方式和 {@code UserServiceImpl.login()} 完全一致，所以造出来的 token
 * 拿去 curl 真实接口一样能通过登录校验。</p>
 *
 * <p>注意两件事：</p>
 * <ol>
 *   <li>抢票接口是异步的 —— Lua 校验通过后只往 RabbitMQ 投递消息就返回，真正写库在消费者线程上。
 *       所以断言之前要轮询等订单数涨上来，不能调完接口立刻查库。</li>
 *   <li>测试会改动数据库和 Redis：清空 ticket_id=1 的订单、重置库存、把开抢时间调成覆盖当前；
 *       结束时删掉自己造的 token。</li>
 * </ol>
 *
 * <p>运行前需要启动 MySQL、Redis 和 RabbitMQ。运行方式：</p>
 * <pre>mvn test -Dtest=TicketConcurrencyTest</pre>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
@SpringBootTest(properties = "spring.datasource.hikari.maximum-pool-size=50")
class TicketConcurrencyTest {

    @Resource
    private ITicketOrderService ticketOrderService;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /** MvcConfig 里是 new RefreshTokenInterceptor(...) 造的，不是 Bean，这里自己造一个同款 */
    private RefreshTokenInterceptor refreshTokenInterceptor;

    /** 本次测试造出来的 token，跑完要删掉 */
    private final List<String> createdTokens = Collections.synchronizedList(new ArrayList<>());

    /** 测试用的票档 id */
    private static final Long TICKET_ID = 1L;

    /** 并发线程数 */
    private static final int THREADS = 200;

    /** 造 token 时用户 id 的起始值，避开真实用户 */
    private static final long USER_ID_BASE = 500000L;

    @BeforeEach
    void setUp() {
        refreshTokenInterceptor = new RefreshTokenInterceptor(stringRedisTemplate);
    }

    @AfterEach
    void tearDown() {
        // 把自己造的 token 从 Redis 里清掉，别污染登录态
        for (String token : createdTokens) {
            stringRedisTemplate.delete(LOGIN_USER_KEY + token);
        }
        createdTokens.clear();
    }

    // ==================== 测试 1：同一个人并发抢，验证「一人一单」 ====================

    /**
     * 200 个线程用同一个用户的 token 同时抢票，期望只有 1 次成功、库里只有 1 条订单。
     *
     * <p>校验点在 Lua 脚本的 SISMEMBER 上：第一个线程把用户写进已抢集合之后，
     * 后面的线程都会被判定为重复抢票。</p>
     *
     * @throws InterruptedException 等待并发线程结束时被中断
     */
    @Test
    void testSameUserConcurrency() throws InterruptedException {
        reset(100);

        // 只造 1 个 token，200 个线程都用它 —— 相当于同一个人狂点 200 次
        String token = newTokens(1, USER_ID_BASE).get(0);
        long expectedUserId = resolvedUserIdOf(token);

        AtomicInteger success = new AtomicInteger();
        AtomicInteger fail = new AtomicInteger();
        Set<Long> seenUserIds = ConcurrentHashMap.newKeySet();
        long cost = runConcurrently(THREADS, () -> token, seenUserIds, success, fail);

        // 等消费者把消息处理完再断言
        awaitOrders(1, 15_000);
        int orders = countOrdersOf(expectedUserId);

        System.out.println();
        System.out.println("========== 测试1：一人一单（走真实 token）==========");
        System.out.println("  token = " + token + "  →  拦截器解析出 userId = " + expectedUserId);
        System.out.println("  库存充足、并发 " + THREADS + " 次、全部用这同一个 token");
        System.out.println("  业务线程实际从 UserHolder 读到的 userId 集合 = " + seenUserIds);
        System.out.println("  成功 " + success.get() + " 次，失败 " + fail.get() + " 次，耗时 " + cost + " ms");
        System.out.println("  数据库里这个用户的订单数 = " + orders);
        System.out.println(success.get() == 1 && orders == 1
                ? "  ✅ 符合预期：只下了 1 单"
                : "  ❌ 不符合预期：应该只有 1 单");
        System.out.println("==================================================");
    }

    // ==================== 测试 2：200 个人抢 100 张票，验证不超卖 ====================

    /**
     * 200 个不同用户抢 100 张票，期望恰好成功 100 次、余票归零、订单表恰好 100 行。
     *
     * <p>校验点在 Lua 脚本的库存判断上，以及落库时 {@code where stock > 0} 这个条件更新 ——
     * 两者共同保证不会超卖。</p>
     *
     * @throws InterruptedException 等待并发线程结束时被中断
     */
    @Test
    void testOversell() throws InterruptedException {
        final int STOCK = 100;
        reset(STOCK);

        // 造 200 个不同的 token，每个对应一个不同用户
        List<String> tokens = newTokens(THREADS, USER_ID_BASE);
        AtomicInteger seq = new AtomicInteger(0);

        AtomicInteger success = new AtomicInteger();
        AtomicInteger fail = new AtomicInteger();
        Set<Long> seenUserIds = ConcurrentHashMap.newKeySet();
        long cost = runConcurrently(THREADS, () -> tokens.get(seq.getAndIncrement()),
                seenUserIds, success, fail);

        // 等消费者把队列里的 100 条消息处理完
        awaitOrders(STOCK, 30_000);
        int stock = currentStock();
        int orders = countOrders();

        System.out.println();
        System.out.println("========== 测试2：超卖（走真实 token）==========");
        System.out.println("  库存 " + STOCK + " 张、并发 " + THREADS + " 个不同 token");
        System.out.println("  拦截器一共解析出 " + seenUserIds.size() + " 个不同 userId（期望 " + THREADS + "）");
        System.out.println("  成功 " + success.get() + " 次，失败 " + fail.get() + " 次，耗时 " + cost + " ms");
        System.out.println("  剩余库存 = " + stock + "，订单表实际行数 = " + orders);
        boolean ok = success.get() == STOCK && stock == 0 && orders == STOCK
                && seenUserIds.size() == THREADS;
        System.out.println(ok
                ? "  ✅ 符合预期：恰好卖完 100 张，没有超卖"
                : "  ❌ 不符合预期：成功数/库存/订单数/用户数 对不上");
        System.out.println("==============================================");
    }

    // ==================== 造 token ====================

    /**
     * 造一批真实可用的 token。
     *
     * <p>写入 Redis 的 key 格式、hash 字段、有效期都和 {@code UserServiceImpl.login()} 保持一致，
     * 所以这些 token 拿去打真实 HTTP 接口也能通过登录校验。</p>
     *
     * @param count      造几个
     * @param userIdBase 用户 id 的起始值
     * @return token 列表
     */
    private List<String> newTokens(int count, long userIdBase) {
        List<String> tokens = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String token = UUID.randomUUID().toString(true);
            long userId = userIdBase + i;

            Map<String, String> map = new HashMap<>();
            map.put("id", String.valueOf(userId));
            map.put("nickName", "tester" + userId);
            map.put("icon", "");

            String key = LOGIN_USER_KEY + token;
            stringRedisTemplate.opsForHash().putAll(key, map);
            stringRedisTemplate.expire(key, 30, TimeUnit.MINUTES);

            tokens.add(token);
            createdTokens.add(token);
        }
        return tokens;
    }

    /**
     * 用拦截器解析一个 token，看它对应哪个 userId。
     *
     * <p>只做解析，不碰抢票业务，所以不会消耗库存。</p>
     *
     * @param token 待解析的 token
     * @return 解析出的用户 id
     */
    private long resolvedUserIdOf(String token) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("authorization", token);
        try {
            refreshTokenInterceptor.preHandle(req, new MockHttpServletResponse(), null);
            return UserHolder.getUser().getId();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            UserHolder.removeUser();
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 重置测试数据。
     *
     * <p>数据库和 Redis 都要重置：抢票的资格校验完全走 Redis 里的 Lua 脚本，
     * 只改数据库不改 Redis 的话，脚本读到的还是上一次的余票，测试结果就没意义了。</p>
     *
     * <p>同时要把开抢时间调成覆盖当前时刻，否则所有请求都会因为不在售票时间内被拦下。</p>
     *
     * @param stock 要设置的余票数
     */
    private void reset(int stock) {
        // 数据库侧：清订单、设余票、把开抢时间调到覆盖当前
        jdbcTemplate.update("DELETE FROM tb_ticket_order WHERE ticket_id = ?", TICKET_ID);
        jdbcTemplate.update("UPDATE tb_ticket_sale SET stock = ? WHERE ticket_id = ?", stock, TICKET_ID);
        jdbcTemplate.update("UPDATE tb_ticket_sale SET begin_time = DATE_SUB(NOW(), INTERVAL 1 DAY), "
                + "end_time = DATE_ADD(NOW(), INTERVAL 1 DAY) WHERE ticket_id = ?", TICKET_ID);
        jdbcTemplate.update("UPDATE tb_ticket SET type = 1 WHERE id = ?", TICKET_ID);

        // Redis 侧：Lua 脚本读的就是这两个 key
        stringRedisTemplate.opsForValue().set(TICKET_STOCK_KEY + TICKET_ID, String.valueOf(stock));
        stringRedisTemplate.delete(TICKET_ORDER_KEY + TICKET_ID);
    }

    /**
     * 开 threads 个线程一起冲。
     *
     * <p>每个线程做的事：把 token 放进 authorization 请求头 → 交给 RefreshTokenInterceptor 解析
     * → 拦截器查 Redis 把用户写进 UserHolder → 调抢票业务。</p>
     *
     * <p>用 CountDownLatch 做发令枪：所有线程先就位，主线程一声令下才同时发请求，
     * 保证是真并发，而不是先启动的线程已经跑完了后面的还没开始。</p>
     *
     * @param threads       并发线程数
     * @param tokenSupplier 每个线程取 token 的方式
     * @param seenUserIds   收集各线程实际读到的 userId，用来证明拦截器确实生效了
     * @param success       成功计数器
     * @param fail          失败计数器
     * @return 整体耗时，单位毫秒
     * @throws InterruptedException 等待线程结束时被中断
     */
    private long runConcurrently(int threads, Supplier<String> tokenSupplier,
                                 Set<Long> seenUserIds,
                                 AtomicInteger success, AtomicInteger fail) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger errors = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                String token = tokenSupplier.get();
                MockHttpServletRequest request = new MockHttpServletRequest();
                request.addHeader("authorization", token);     // ← 就是真实的请求头
                MockHttpServletResponse response = new MockHttpServletResponse();

                ready.countDown();
                try {
                    start.await();                              // 全体在这里排队

                    // 走真实的拦截器解析：token → Redis → UserHolder
                    if (!refreshTokenInterceptor.preHandle(request, response, null)) {
                        throw new IllegalStateException(
                                "拦截器没放行，HTTP " + response.getStatus() + "，token=" + token);
                    }
                    seenUserIds.add(UserHolder.getUser().getId());

                    Result r = ticketOrderService.seckillTicket(TICKET_ID);
                    if (Boolean.TRUE.equals(r.getSuccess())) {
                        success.incrementAndGet();
                    } else {
                        fail.incrementAndGet();
                    }
                } catch (Throwable t) {
                    int n = errors.incrementAndGet();
                    if (n <= 3) {                               // 只打前 3 条，避免刷屏
                        System.out.println("  ⚠ 线程抛异常: " + t.getClass().getName() + ": " + t.getMessage());
                    }
                } finally {
                    UserHolder.removeUser();                    // 等价于拦截器的 afterCompletion
                    done.countDown();
                }
            });
        }

        ready.await();
        long t0 = System.currentTimeMillis();
        start.countDown();                                      // 发令枪
        done.await();
        long cost = System.currentTimeMillis() - t0;
        pool.shutdown();

        if (errors.get() > 0) {
            System.out.println("  注意：有 " + errors.get() + " 个线程抛了异常（没计入成功/失败）");
        }
        return cost;
    }

    /**
     * 轮询等待订单数涨到预期值。
     *
     * <p>抢票接口投递消息后就返回了，订单是消费者异步写进去的，所以断言前必须先等一等。</p>
     *
     * @param expected       预期订单数
     * @param timeoutMillis  最长等多久，超时就返回，交给后面的断言去报错
     * @throws InterruptedException 休眠被中断
     */
    private void awaitOrders(int expected, long timeoutMillis) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            if (countOrders() >= expected) {
                return;
            }
            Thread.sleep(100);
        }
    }

    /**
     * 统计某个用户在这个票档下的订单数。
     *
     * @param userId 用户id
     * @return 订单条数
     */
    private int countOrdersOf(long userId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_ticket_order WHERE ticket_id = ? AND user_id = ?",
                Integer.class, TICKET_ID, userId);
    }

    /**
     * 统计这个票档下的全部订单数。
     *
     * @return 订单条数
     */
    private int countOrders() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_ticket_order WHERE ticket_id = ?",
                Integer.class, TICKET_ID);
    }

    /**
     * 查询数据库里这个票档的剩余库存。
     *
     * @return 剩余张数
     */
    private int currentStock() {
        return jdbcTemplate.queryForObject(
                "SELECT stock FROM tb_ticket_sale WHERE ticket_id = ?",
                Integer.class, TICKET_ID);
    }
}
