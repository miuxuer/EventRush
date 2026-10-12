package com.miuxuer.eventrush.controller;

import com.miuxuer.eventrush.dto.Result;
import com.miuxuer.eventrush.service.ITicketOrderService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 抢票下单接口。
 *
 * <p>需要登录才能抢票，所以不在拦截器白名单里。</p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
@RestController
@RequestMapping("/ticket-order")
public class TicketOrderController {

    @Resource
    private ITicketOrderService ticketOrderService;

    /**
     * 抢票。
     *
     * <p>接口本身只做 Redis 里的资格校验并投递消息，不做数据库写入，所以响应很快。
     * 返回的订单号是异步生成的，前端拿它去查询最终的抢票结果。</p>
     *
     * @param ticketId 票档id
     * @return 成功时 data 为订单号；失败时 errorMsg 说明是售罄还是重复抢票
     */
    @PostMapping("seckill/{id}")
    public Result seckillTicket(@PathVariable("id") Long ticketId) {
        return ticketOrderService.seckillTicket(ticketId);
    }
}
