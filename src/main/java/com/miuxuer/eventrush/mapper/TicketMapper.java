package com.miuxuer.eventrush.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miuxuer.eventrush.entity.Ticket;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 票档数据访问接口，对应实体 {@link Ticket} 和数据库表 tb_ticket。
 * <p>
 * 普通单表操作继承自 MyBatis-Plus 的 BaseMapper；
 * 查询演出下的票档时需要额外带出抢票场次信息（库存、开票时间），因此在此定义了自定义查询。
 * </p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
public interface TicketMapper extends BaseMapper<Ticket> {

    /**
     * 查询指定演出下已上架（status = 1）的票档，并以 LEFT JOIN 关联 tb_ticket_sale，
     * 一并带出余票 stock 与开票起止时间（SQL 定义在 resources/mapper/TicketMapper.xml）。
     *
     * @param eventId 演出 id
     * @return 该演出下已上架的票档列表
     */
    List<Ticket> queryTicketOfEvent(@Param("eventId") Long eventId);
}
