package com.miuxuer.eventrush.controller;

import com.miuxuer.eventrush.dto.Result;
import com.miuxuer.eventrush.entity.Ticket;
import com.miuxuer.eventrush.service.ITicketService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 票档接口。
 *
 * <p>查询是公开的，新增属于后台管理操作。</p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
@RestController
@RequestMapping("/ticket")
public class TicketController {

    @Resource
    private ITicketService ticketService;

    /**
     * 新增一个普通票档（不参与抢票，只用来展示和直接购买）。
     *
     * @param ticket 票档信息
     * @return 新票档的 id
     */
    @PostMapping
    public Result addTicket(@RequestBody Ticket ticket) {
        ticketService.save(ticket);
        return Result.ok(ticket.getId());
    }

    /**
     * 新增一个参与抢票的票档。
     *
     * <p>除了票档本身，还会写入库存、开票起止时间，并把余票预热到 Redis。</p>
     *
     * @param ticket 票档信息，需要带上 stock、beginTime、endTime
     * @return 新票档的 id
     */
    @PostMapping("seckill")
    public Result addTicketSale(@RequestBody Ticket ticket) {
        ticketService.addTicketSale(ticket);
        return Result.ok(ticket.getId());
    }

    /**
     * 查询某个演出下的全部在售票档。
     *
     * @param eventId 演出id
     * @return 票档列表，含余票和开票时间
     */
    @GetMapping("/list/{eventId}")
    public Result queryTicketOfEvent(@PathVariable("eventId") Long eventId) {
        return ticketService.queryTicketOfEvent(eventId);
    }
}
