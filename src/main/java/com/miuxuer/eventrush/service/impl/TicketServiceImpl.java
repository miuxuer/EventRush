package com.miuxuer.eventrush.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.miuxuer.eventrush.dto.Result;
import com.miuxuer.eventrush.entity.Ticket;
import com.miuxuer.eventrush.entity.TicketSale;
import com.miuxuer.eventrush.mapper.TicketMapper;
import com.miuxuer.eventrush.service.ITicketSaleService;
import com.miuxuer.eventrush.service.ITicketService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.miuxuer.eventrush.utils.RedisConstants.TICKET_STOCK_KEY;

/**
 * 票档服务实现。
 *
 * @author miuxuer
 * @since 2026-10-12
 */
@Service
public class TicketServiceImpl extends ServiceImpl<TicketMapper, Ticket> implements ITicketService {

    @Resource
    private ITicketSaleService ticketSaleService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 查询某个演出下的全部在售票档。
     *
     * <p>SQL 写在 TicketMapper.xml 里，用左连接把票档主表和库存表的数据拼成一条返回，
     * 前端拿到的每个票档既能看到价格也能看到余票和开票时间。</p>
     *
     * @param eventId 演出id
     * @return 票档列表
     */
    @Override
    public Result queryTicketOfEvent(Long eventId) {
        List<Ticket> tickets = getBaseMapper().queryTicketOfEvent(eventId);
        return Result.ok(tickets);
    }

    /**
     * 新增一个参与抢票的票档。
     *
     * <p>三件事要一起做，所以加了事务：</p>
     * <ol>
     *   <li>写票档主表 tb_ticket；</li>
     *   <li>写库存表 tb_ticket_sale（余票、开票起止时间）；</li>
     *   <li>把余票预热进 Redis，抢票时的 Lua 脚本读的就是这个值。</li>
     * </ol>
     *
     * @param ticket 票档信息，需要带上 stock、beginTime、endTime
     */
    @Override
    @Transactional
    public void addTicketSale(Ticket ticket) {
        // 1. 保存票档
        save(ticket);

        // 2. 保存这个票档的库存和开票时间，和票档主表是一对一关系
        TicketSale ticketSale = new TicketSale();
        ticketSale.setTicketId(ticket.getId());
        ticketSale.setStock(ticket.getStock());
        ticketSale.setBeginTime(ticket.getBeginTime());
        ticketSale.setEndTime(ticket.getEndTime());
        ticketSaleService.save(ticketSale);

        // 3. 把库存预热到 redis，key 是票档 id，值是余票数
        stringRedisTemplate.opsForValue().set(TICKET_STOCK_KEY + ticket.getId(), ticket.getStock().toString());
    }
}
