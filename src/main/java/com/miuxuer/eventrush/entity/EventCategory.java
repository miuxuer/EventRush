package com.miuxuer.eventrush.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 演出分类实体，对应数据库表 tb_event_category。
 * <p>
 * 用于给演出分组，例如演唱会、音乐节、话剧、体育赛事等。
 * 分类列表查询非常频繁，因此会整体缓存到 Redis 中，避免每次都查数据库。
 * </p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_event_category")
public class EventCategory implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 类型名称
     */
    private String name;

    /**
     * 图标
     */
    private String icon;

    /**
     * 顺序，数值越小越靠前展示
     */
    private Integer sort;

    /**
     * 创建时间，不返回给前端
     */
    @JsonIgnore
    private LocalDateTime createTime;

    /**
     * 更新时间，不返回给前端
     */
    @JsonIgnore
    private LocalDateTime updateTime;


}
