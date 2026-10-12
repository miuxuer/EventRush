package com.miuxuer.eventrush.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.miuxuer.eventrush.dto.Result;
import com.miuxuer.eventrush.entity.Ticket;

/**
 * 票档服务。
 *
 * <p>一个演出下面可以有多个票档（看台票、内场票、VIP票……），每个票档有自己的售价和余票。</p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
public interface ITicketService extends IService<Ticket> {

    /**
     * 查询某个演出下的全部在售票档。
     *
     * @param eventId 演出id
     * @return 票档列表，每个票档带上余票和开票起止时间
     */
    Result queryTicketOfEvent(Long eventId);

    /**
     * 新增一个参与抢票的票档。
     *
     * <p>会同时写两张表：票档主表和它的库存/开票时间表；并把余票预热到 Redis，
     * 这样抢票时 Lua 脚本才能直接读到库存。</p>
     *
     * @param ticket 票档信息，含库存、开票时间
     */
    void addTicketSale(Ticket ticket);
}
