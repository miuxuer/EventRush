package com.miuxuer.eventrush.mapper;

import com.miuxuer.eventrush.entity.TicketSale;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 抢票票档（场次信息）数据访问接口，对应实体 {@link TicketSale} 和数据库表 tb_ticket_sale。
 * <p>
 * 继承 MyBatis-Plus 的 BaseMapper；抢票时对余票 stock 的扣减通过业务层的 Lua 脚本 + 乐观锁完成，
 * 不在此处直接写扣减 SQL。
 * </p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
public interface TicketSaleMapper extends BaseMapper<TicketSale> {

}
