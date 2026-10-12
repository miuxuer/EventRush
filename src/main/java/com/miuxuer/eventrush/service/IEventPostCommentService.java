package com.miuxuer.eventrush.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.miuxuer.eventrush.dto.Result;
import com.miuxuer.eventrush.entity.EventPostComment;

/**
 * 观演笔记的评论服务。
 *
 * <p>评论只有两级：一级评论的 {@code parentId} 为 0；回复挂在某条一级评论下，
 * 此时 {@code parentId} 指向那条一级评论的 id，{@code answerId} 指向被回复的那条评论的 id
 * （回复一级评论时 answerId 就等于 parentId）。</p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
public interface IEventPostCommentService extends IService<EventPostComment> {

    /**
     * 发表评论或回复。
     *
     * <p>作者不用前端传，服务端从登录态里取；同时会把笔记上的评论数 +1。</p>
     *
     * @param comment 评论内容，postId 必填，parentId / answerId 留空表示发表一级评论
     * @return 成功时 data 为评论 id
     */
    Result saveComment(EventPostComment comment);

    /**
     * 分页查询某篇笔记的评论，按发表时间倒序。
     *
     * <p>返回的每条评论都补上了评论者的昵称、头像；如果是回复，还会补上被回复人的昵称。</p>
     *
     * @param postId  笔记id
     * @param current 页码，从 1 开始
     * @return 评论列表
     */
    Result queryCommentOfPost(Long postId, Integer current);

    /**
     * 删除评论，只能删自己发的。
     *
     * <p>删一级评论时会连它下面的回复一起删掉，并同步扣减笔记上的评论数。</p>
     *
     * @param id 评论id
     * @return 成功返回 ok；评论不存在或不是自己的返回失败提示
     */
    Result deleteComment(Long id);
}
