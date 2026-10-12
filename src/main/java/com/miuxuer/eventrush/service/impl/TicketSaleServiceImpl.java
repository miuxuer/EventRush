package com.miuxuer.eventrush.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.miuxuer.eventrush.entity.TicketSale;
import com.miuxuer.eventrush.mapper.TicketSaleMapper;
import com.miuxuer.eventrush.service.ITicketSaleService;
import org.springframework.stereotype.Service;

/**
 * 票档库存服务实现。
 *
 * <p>没有额外的业务逻辑，直接复用 MyBatis-Plus 提供的基础 CRUD。
 * 下单时的「扣减库存」就是在 {@code TicketOrderServiceImpl} 里通过这里拼条件更新完成的。</p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
@Service
public class TicketSaleServiceImpl extends ServiceImpl<TicketSaleMapper, TicketSale> implements ITicketSaleService {

}
