package com.miuxuer.eventrush.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.miuxuer.eventrush.entity.TicketSale;

/**
 * 票档库存服务。
 *
 * <p>只维护 tb_ticket_sale 一张表，抢票时扣减库存就是通过这里操作的。</p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
public interface ITicketSaleService extends IService<TicketSale> {

}
