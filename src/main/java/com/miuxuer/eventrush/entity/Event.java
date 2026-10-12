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
 * 演出/赛事项目实体，对应数据库表 tb_event。
 * <p>
 * 除了名称、分类、门票均价等基础信息外，还保存了演出场馆的经纬度（x、y），
 * 用于写入 Redis GEO，实现"按距离由近到远搜索演出"的功能。
 * </p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_event")
public class Event implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 演出名称
     */
    private String name;

    /**
     * 所属演出分类的 id（关联 tb_event_category.id）
     */
    private Long categoryId;

    /**
     * 演出图片，多个图片以','隔开
     */
    private String images;

    /**
     * 演出所在城区，例如陆家嘴
     */
    private String area;

    /**
     * 地址
     */
    private String address;

    /**
     * 经度
     */
    private Double x;

    /**
     * 纬度
     */
    private Double y;

    /**
     * 均价，取整数
     */
    private Long avgPrice;

    /**
     * 销量
     */
    private Integer sold;

    /**
     * 评论数量
     */
    private Integer comments;

    /**
     * 评分，1~5分，乘10保存，避免小数
     */
    private Integer score;

    /**
     * 演出时间，例如 10:00-22:00
     */
    private String showTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 当前坐标到本演出的距离，非数据库字段；仅按距离搜索时由查询结果回填
     */
    @TableField(exist = false)
    private Double distance;
}
