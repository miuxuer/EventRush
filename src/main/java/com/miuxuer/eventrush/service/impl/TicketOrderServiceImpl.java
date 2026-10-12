package com.miuxuer.eventrush.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.miuxuer.eventrush.config.RabbitMQConfig;
import com.miuxuer.eventrush.dto.Result;
import com.miuxuer.eventrush.entity.TicketOrder;
import com.miuxuer.eventrush.mapper.TicketOrderMapper;
import com.miuxuer.eventrush.service.ITicketOrderService;
import com.miuxuer.eventrush.service.ITicketSaleService;
import com.miuxuer.eventrush.utils.RedisIdWorker;
import com.miuxuer.eventrush.utils.UserHolder;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Collections;

/**
 * 抢票下单服务实现。
 *
 * <p>整体流程分两步，尽量把压力挡在数据库之前：</p>
 * <pre>
 *   1. 同步：请求线程执行 Lua 脚本，在 Redis 里原子完成「查余票 + 查一人一单 + 扣余票 + 记录用户」，
 *      校验不通过当场返回；通过则生成订单号，把下单消息投递到 RabbitMQ，直接返回订单号。
 *   2. 异步：消费者线程从队列取消息，加 Redisson 分布式锁后开事务落库（一人一单复核 + 扣库存 + 写订单）。
 * </pre>
 *
 * <p>之所以拆成两步，是因为抢票瞬间的并发量远大于数据库能承受的写入量。Lua 脚本保证不会超卖，
 * 队列负责削峰，真正打到 MySQL 的只有确实抢到票的那部分请求。</p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
@Service
@Slf4j
public class TicketOrderServiceImpl extends ServiceImpl<TicketOrderMapper, TicketOrder> implements ITicketOrderService {

    @Autowired
    private ITicketSaleService ticketSaleService;

    @Autowired
    private RedisIdWorker redisIdWorker;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 自身的代理对象。
     *
     * <p>{@link #createTicketOrder} 上标了 {@code @Transactional}，类内部直接 this 调用属于自调用，
     * 走不到代理，事务不会生效。</p>
     *
     * <p>消费逻辑跑在 RabbitMQ 监听容器线程上，那里没有 {@code AopContext}，
     * 拿不到 {@code AopContext.currentProxy()}，所以干脆用 {@code @Lazy} 把自己注入进来，
     * 调用时再真正解析，也就顺带绕开了循环依赖。</p>
     */
    @Autowired
    @Lazy
    private ITicketOrderService self;

    /** 抢票资格校验脚本，脚本见 resources/seckill.lua */
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    /**
     * 抢票。
     *
     * <p>本方法只做「抢资格」这一件事：够不够票、是不是重复抢，全部由 Redis 里的 Lua 脚本原子判定。
     * 判定通过后不直接写库，而是把下单动作丢给 RabbitMQ，立刻把订单号返回给前端，
     * 前端拿订单号轮询抢票结果即可。</p>
     *
     * @param ticketId 票档id
     * @return 成功时 data 为订单号；失败时 errorMsg 说明是「票已售罄」还是「不能重复抢票」
     */
    @Override
    public Result seckillTicket(Long ticketId) {
        // 1. 从 ThreadLocal 里取当前登录用户，未登录会被登录拦截器提前拦掉
        Long userId = UserHolder.getUser().getId();
        // 2. 生成全局唯一订单号（时间戳 + Redis 自增序列）
        long orderId = redisIdWorker.nextId("order");

        // 3. 执行 Lua 脚本。
        //    脚本内部依次做：余票是否充足 → 用户是否已经抢过 → 扣减余票 → 记录该用户已抢。
        //    这几步必须原子，否则并发下会出现超卖或者一人抢到多张。
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                ticketId.toString(),
                userId.toString()
        );

        // 4. 脚本返回 0 才代表校验通过，1 是没票了，2 是重复抢票
        if (result != 0L) {
            return Result.fail(result == 1 ? "票已售罄" : "不能重复抢票");
        }

        // 5. 校验通过，组装下单消息投递到 RabbitMQ，真正写库交给消费者异步完成
        TicketOrder ticketOrder = TicketOrder.builder()
                .id(orderId)
                .userId(userId)
                .ticketId(ticketId)
                .build();
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.TICKET_ORDER_EXCHANGE,
                RabbitMQConfig.TICKET_ORDER_ROUTING_KEY,
                ticketOrder
        );

        // 6. 返回订单号，前端凭它查询抢票结果
        return Result.ok(orderId);
    }

    /**
     * 下单消息的消费者。
     *
     * <p>用「直连交换机 + 单队列 + 手动 ACK + 死信队列」的组合：</p>
     * <ul>
     *   <li>处理成功 → basicAck 确认，消息从队列里移除；</li>
     *   <li>处理失败且还没到重试上限 → 带上重试次数重新投递，再 ACK 掉当前这条；
     *       这一层对应原来 Redis Stream 里 pending-list 的重试逻辑；</li>
     *   <li>重试次数用尽 → basicNack 且不重新入队，消息被死信交换机转到 ticket.order.dlq，
     *       留着人工排查，不会悄无声息地丢掉。</li>
     * </ul>
     *
     * @param ticketOrder 下单消息体，字段就是 {@link TicketOrder} 的 id / userId / ticketId
     * @param message     AMQP 原始消息，用来拿 deliveryTag 和重试次数
     * @param channel     当前信道，手动 ACK 必须通过它操作
     */
    @RabbitListener(queues = RabbitMQConfig.TICKET_ORDER_QUEUE, ackMode = "MANUAL")
    public void handleTicketOrderMessage(TicketOrder ticketOrder, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            // 1. 加锁 + 事务落库
            handleTicketOrder(ticketOrder);
            // 2. 处理成功，确认消息
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("处理抢票订单异常，orderId={}", ticketOrder.getId(), e);
            int retry = retryCountOf(message);
            if (retry >= RabbitMQConfig.MAX_RETRY) {
                // 3. 重试次数用尽，拒绝并且不重新入队 —— 消息会被转到死信队列
                channel.basicNack(deliveryTag, false, false);
            } else {
                // 4. 还允许重试：重新投递一条带重试次数的消息，然后把当前这条确认掉。
                //    重试次数有上限，不会形成死循环。
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.TICKET_ORDER_EXCHANGE,
                        RabbitMQConfig.TICKET_ORDER_ROUTING_KEY,
                        ticketOrder,
                        msg -> {
                            msg.getMessageProperties().setHeader(RabbitMQConfig.RETRY_HEADER, retry + 1);
                            return msg;
                        }
                );
                channel.basicAck(deliveryTag, false);
            }
        }
    }

    /**
     * 死信队列的消费者。
     *
     * <p>能进到这里说明已经重试了 {@link RabbitMQConfig#MAX_RETRY} 次仍然失败，
     * 目前只打日志告警，订单需要人工介入处理。生产环境一般会配合钉钉/企业微信机器人推送。</p>
     *
     * @param ticketOrder 处理失败的下单消息
     */
    @RabbitListener(queues = RabbitMQConfig.TICKET_ORDER_DLQ)
    public void handleDeadLetterOrder(TicketOrder ticketOrder) {
        log.error("抢票订单重试耗尽进入死信队列，需人工处理：{}", ticketOrder);
    }

    /**
     * 读取消息头里记录的重试次数。
     *
     * @param message AMQP 原始消息
     * @return 已重试次数，第一次消费返回 0
     */
    private int retryCountOf(Message message) {
        Object retry = message.getMessageProperties().getHeader(RabbitMQConfig.RETRY_HEADER);
        return retry == null ? 0 : ((Number) retry).intValue();
    }

    /**
     * 加分布式锁后创建订单。
     *
     * <p>锁的粒度是「用户」：同一个用户的多个下单消息只允许一个在跑，防止并发写出重复订单。
     * 锁失败说明这个用户已经有另一条消息在处理中，这条直接放弃即可 ——
     * 反正 Lua 脚本和订单表的唯一性校验都会保证他最终只有一单。</p>
     *
     * @param ticketOrder 下单消息
     */
    private void handleTicketOrder(TicketOrder ticketOrder) {
        // 获取用户
        Long userId = ticketOrder.getUserId();
        // 创建锁对象
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        // 尝试获取锁，重试不等待
        boolean result = lock.tryLock();
        if (!result) {
            log.error("不允许重复下单，userId={}", userId);
            return;
        }

        try {
            // 必须用代理对象调用，事务才会生效
            self.createTicketOrder(ticketOrder);
        } finally {
            // 释放锁
            lock.unlock();
        }
    }

    /**
     * 真正把订单写进数据库：一人一单复核 + 扣减余票 + 保存订单。
     *
     * <p>加了 {@code @Transactional}，三步里任何一步失败整单回滚，不会出现「扣了票却没生成订单」的情况。</p>
     *
     * @param ticketOrder 下单消息
     */
    @Override
    @Transactional
    public void createTicketOrder(TicketOrder ticketOrder) {
        // 1. 一人一单：同一个用户对同一个票档只允许存在一条订单
        Long userId = ticketOrder.getUserId();
        Long count = query().eq("user_id", userId).eq("ticket_id", ticketOrder.getTicketId()).count();
        if (count > 0) {
            log.error("用户已经抢过该票档了，userId={}", userId);
            return;
        }

        // 2. 扣减余票。
        //    stock > 0 这个条件相当于乐观锁：只有确实还有余票，这条 update 才能影响一行。
        //    相比「先查库存再比对」的 CAS 写法，这样不会出现「明明还有票却因为并发而失败」的问题。
        boolean success = ticketSaleService.update()
                .setSql("stock = stock - 1")
                .eq("ticket_id", ticketOrder.getTicketId())
                .gt("stock", 0)
                .update();
        if (!success) {
            log.error("票已售罄，ticketId={}", ticketOrder.getTicketId());
            return;
        }

        // 3. 保存订单
        save(ticketOrder);
    }
}
