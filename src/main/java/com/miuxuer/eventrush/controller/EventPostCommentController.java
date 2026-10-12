package com.miuxuer.eventrush.controller;

import com.miuxuer.eventrush.dto.Result;
import com.miuxuer.eventrush.entity.EventPostComment;
import com.miuxuer.eventrush.service.IEventPostCommentService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 观演笔记评论接口。
 *
 * <p>评论只支持两级：直接评论笔记，或者在一条评论下面回复。</p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
@RestController
@RequestMapping("/post-comments")
public class EventPostCommentController {

    @Resource
    private IEventPostCommentService postCommentService;

    /**
     * 发表评论或回复。
     *
     * <p>发一级评论时只有 postId 和 content，parentId 留空；
     * 回复时再带上被回复评论的 id 作为 parentId。</p>
     *
     * @param comment 评论内容
     * @return 新评论的 id
     */
    @PostMapping
    public Result saveComment(@RequestBody EventPostComment comment) {
        return postCommentService.saveComment(comment);
    }

    /**
     * 分页查询某篇笔记的评论。
     *
     * @param postId  笔记id
     * @param current 页码，默认第一页
     * @return 评论列表，一级评论后面跟着它自己的回复
     */
    @GetMapping("/of/{postId}")
    public Result queryCommentOfPost(@PathVariable("postId") Long postId,
                                     @RequestParam(value = "current", defaultValue = "1") Integer current) {
        return postCommentService.queryCommentOfPost(postId, current);
    }

    /**
     * 删除评论，只能删自己发的。
     *
     * @param id 评论id
     * @return 无
     */
    @DeleteMapping("/{id}")
    public Result deleteComment(@PathVariable("id") Long id) {
        return postCommentService.deleteComment(id);
    }
}
