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
 * 用户关注关系实体，对应数据库表 tb_follow。
 * <p>
 * 一行记录表示"某个用户关注了另一个用户"，
 * 同时会把这些关系写入 Redis Set，支撑共同关注、Feed 流推送等场景。
 * </p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_follow")
public class Follow implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 发起关注的用户 id
     */
    private Long userId;

    /**
     * 被关注的用户 id
     */
    private Long followUserId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;


}
