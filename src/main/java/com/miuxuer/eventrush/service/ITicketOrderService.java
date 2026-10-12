package com.miuxuer.eventrush.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.miuxuer.eventrush.dto.Result;
import com.miuxuer.eventrush.entity.TicketOrder;

/**
 * 抢票订单服务。
 *
 * @author miuxuer
 * @since 2026-10-12
 */
public interface ITicketOrderService extends IService<TicketOrder> {

    /**
     * 抢票：在 Redis 里原子校验并预占名额，然后把下单消息投递到 RabbitMQ 异步落库。
     *
     * @param ticketId 票档id
     * @return 成功时 data 为订单号，前端凭它查询抢票结果
     */
    Result seckillTicket(Long ticketId);

    /**
     * 真正创建订单：一人一单复核 + 扣减余票 + 写入订单表。
     *
     * <p>由 RabbitMQ 消费者调用，方法上带事务，必须通过代理对象调用才生效。</p>
     *
     * @param ticketOrder 下单消息
     */
    void createTicketOrder(TicketOrder ticketOrder);
}
