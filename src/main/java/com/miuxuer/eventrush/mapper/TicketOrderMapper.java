package com.miuxuer.eventrush.mapper;

import com.miuxuer.eventrush.entity.TicketOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 抢票订单数据访问接口，对应实体 {@link TicketOrder} 和数据库表 tb_ticket_order。
 * <p>
 * 继承 MyBatis-Plus 的 BaseMapper，订单落库时主键由业务代码（RedisIdWorker）显式赋值。
 * </p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
public interface TicketOrderMapper extends BaseMapper<TicketOrder> {

}
