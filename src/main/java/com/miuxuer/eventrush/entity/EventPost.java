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
 * 观演笔记实体（演出资讯/用户观演分享），对应数据库表 tb_event_post。
 * <p>
 * 点赞数 liked 除了存在数据库外，还会在 Redis 的 ZSet 中维护一份，
 * 用于按点赞数高低排序做"热门笔记"查询。
 * </p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_event_post")
public class EventPost implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联的演出 id
     */
    private Long eventId;

    /**
     * 发布笔记的用户 id
     */
    private Long userId;

    /**
     * 用户头像，非数据库字段，查询时根据 userId 动态填充
     */
    @TableField(exist = false)
    private String icon;

    /**
     * 用户姓名（昵称），非数据库字段，查询时根据 userId 动态填充
     */
    @TableField(exist = false)
    private String name;

    /**
     * 当前登录用户是否已经点赞过这篇笔记，非数据库字段，查询时动态计算
     */
    @TableField(exist = false)
    private Boolean isLike;

    /**
     * 标题
     */
    private String title;

    /**
     * 观演的照片，最多9张，多张以","隔开
     */
    private String images;

    /**
     * 观演的文字描述
     */
    private String content;

    /**
     * 点赞数量
     */
    private Integer liked;

    /**
     * 评论数量
     */
    private Integer comments;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;


}
