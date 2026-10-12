package com.miuxuer.eventrush.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 抢票订单实体，用户抢到票后生成，对应数据库表 tb_ticket_order。
 * <p>
 * 主键 id 不使用数据库自增，而是由 RedisIdWorker 生成的全局唯一 ID 直接赋值，
 * 因此 @TableId 的类型是 IdType.INPUT；这样可以在高并发抢票时先拿到订单号再落库。
 * </p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Builder
@NoArgsConstructor  // 生成 public 无参构造
@AllArgsConstructor // 生成 public 全参构造
@TableName("tb_ticket_order")
public class TicketOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键（由 RedisIdWorker 生成的全局唯一 ID，非数据库自增）
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 下单的用户id
     */
    private Long userId;

    /**
     * 购买的票档 id
     */
    private Long ticketId;

    /**
     * 支付方式 1：余额支付；2：支付宝；3：微信
     */
    private Integer payType;

    /**
     * 订单状态，1：未支付；2：已支付；3：已核销；4：已取消；5：退款中；6：已退款
     */
    private Integer status;

    /**
     * 下单时间
     */
    private LocalDateTime createTime;

    /**
     * 支付时间
     */
    private LocalDateTime payTime;

    /**
     * 核销时间
     */
    private LocalDateTime useTime;

    /**
     * 退款时间
     */
    private LocalDateTime refundTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;


}
