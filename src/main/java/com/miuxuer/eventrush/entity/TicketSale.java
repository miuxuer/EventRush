package com.miuxuer.eventrush.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 抢票票档表，与票档（tb_ticket）是一对一关系，对应数据库表 tb_ticket_sale。
 * <p>
 * 只存放参与抢票的票档的场次信息：可售余票 stock 和开票起止时间 beginTime/endTime。
 * 主键就是票档 id（ticketId），因此使用 IdType.INPUT 由业务代码显式指定。
 * </p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_ticket_sale")
public class TicketSale implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 关联的票档的id
     */
    @TableId(value = "ticket_id", type = IdType.INPUT)
    private Long ticketId;

    /**
     * 可售余票
     */
    private Integer stock;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 开票开始时间
     */
    private LocalDateTime beginTime;

    /**
     * 开票结束时间
     */
    private LocalDateTime endTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;


}
