package com.miuxuer.eventrush.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 票档实体（某场演出下的一种票价档位，如"VIP 区""看台 A 区"），对应数据库表 tb_ticket。
 * <p>
 * payValue 是售价，actualValue 是原价（抵扣金额）；type 为 1 表示这个票档参与抢票活动。
 * stock、beginTime、endTime 不是本表的字段，而是抢票场次信息（tb_ticket_sale）中的内容，
 * 查询时为方便前端展示而一并带出。
 * </p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_ticket")
public class Ticket implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 所属演出的 id
     */
    private Long eventId;

    /**
     * 演出票标题
     */
    private String title;

    /**
     * 副标题
     */
    private String subTitle;

    /**
     * 使用规则
     */
    private String rules;

    /**
     * 支付金额（售价）
     */
    private Long payValue;

    /**
     * 抵扣金额（原价）
     */
    private Long actualValue;

    /**
     * 票档类型，1：参与抢票活动（普通票档不参与）
     */
    private Integer type;

    /**
     * 票档的上下架状态
     */
    private Integer status;

    /**
     * 可售余票，非数据库字段，来自关联的抢票场次信息 tb_ticket_sale
     */
    @TableField(exist = false)
    private Integer stock;

    /**
     * 开票生效时间，非数据库字段，来自关联的抢票场次信息 tb_ticket_sale
     */
    @TableField(exist = false)
    private LocalDateTime beginTime;

    /**
     * 开票失效时间，非数据库字段，来自关联的抢票场次信息 tb_ticket_sale
     */
    @TableField(exist = false)
    private LocalDateTime endTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;


    /**
     * 更新时间
     */
    private LocalDateTime updateTime;


}
