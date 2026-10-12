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
 * 观演笔记评论实体，对应数据库表 tb_event_post_comment。
 * <p>
 * 支持二级回复：parentId 为空（值为 0）表示这是一级评论；
 * parentId 指向某条一级评论、同时 answerId 记录真正被回复的那条评论，则表示这是一条回复。
 * </p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_event_post_comment")
public class EventPostComment implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 发表评论的用户 id
     */
    private Long userId;

    /**
     * 所属观演笔记的 id（关联 tb_event_post.id）
     */
    private Long postId;

    /**
     * 关联的1级评论id，如果是一级评论，则值为0
     */
    private Long parentId;

    /**
     * 被回复的那条评论的 id
     */
    private Long answerId;

    /**
     * 回复的内容
     */
    private String content;

    /**
     * 点赞数
     */
    private Integer liked;

    /**
     * 状态，0：正常，1：被举报，2：禁止查看
     */
    private Boolean status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    // ==================== 以下字段不落库，只在返回给前端时用来补全展示信息 ====================

    /**
     * 评论者昵称
     */
    @TableField(exist = false)
    private String nickName;

    /**
     * 评论者头像
     */
    @TableField(exist = false)
    private String icon;

    /**
     * 被回复人的用户 id，只有回复才有值
     */
    @TableField(exist = false)
    private Long answerUserId;

    /**
     * 被回复人的昵称，只有回复才有值
     */
    @TableField(exist = false)
    private String answerName;

}
