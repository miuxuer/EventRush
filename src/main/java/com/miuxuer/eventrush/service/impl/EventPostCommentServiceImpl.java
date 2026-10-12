package com.miuxuer.eventrush.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.miuxuer.eventrush.dto.Result;
import com.miuxuer.eventrush.entity.EventPostComment;
import com.miuxuer.eventrush.entity.User;
import com.miuxuer.eventrush.mapper.EventPostCommentMapper;
import com.miuxuer.eventrush.service.IEventPostCommentService;
import com.miuxuer.eventrush.service.IEventPostService;
import com.miuxuer.eventrush.service.IUserService;
import com.miuxuer.eventrush.utils.SystemConstants;
import com.miuxuer.eventrush.utils.UserHolder;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 观演笔记评论服务实现。
 *
 * <p>评论分两级，表里靠 {@code parentId} / {@code answerId} 两个字段表达：</p>
 * <pre>
 *   一级评论：parent_id = 0
 *   回复：    parent_id = 一级评论的 id，answer_id = 被回复的那条评论的 id
 * </pre>
 *
 * <p>也就是说只支持「回复评论」，不支持无限层级嵌套。这样查询时一次就能把整棵树捞出来，
 * 不用递归，前端渲染也只处理两层。</p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
@Service
public class EventPostCommentServiceImpl extends ServiceImpl<EventPostCommentMapper, EventPostComment>
        implements IEventPostCommentService {

    @Resource
    private IUserService userService;

    @Resource
    private IEventPostService postService;

    /**
     * 发表评论或回复。
     *
     * <p>两件事必须一起成功：插入评论、给笔记的评论数 +1，所以加了事务。
     * 评论数存在笔记表上（{@code tb_event_post.comments}），是一种冗余设计 ——
     * 好处是列表页不用每次去 count 评论表，代价就是这里得手动维护。</p>
     *
     * @param comment 评论内容，postId 必填
     * @return 成功时 data 为评论 id；内容为空或笔记 id 缺失时返回失败提示
     */
    @Override
    @Transactional
    public Result saveComment(EventPostComment comment) {
        if (comment.getPostId() == null) {
            return Result.fail("笔记id不能为空");
        }
        if (comment.getContent() == null || comment.getContent().isBlank()) {
            return Result.fail("评论内容不能为空");
        }

        // 作者从登录态里取，不接受前端传入，避免伪造他人评论
        comment.setUserId(UserHolder.getUser().getId());

        // 没传 parentId 就当成一级评论处理
        if (comment.getParentId() == null) {
            comment.setParentId(0L);
        }
        if (comment.getAnswerId() == null) {
            // 回复一级评论时，被回复的就是那条一级评论本身
            comment.setAnswerId(comment.getParentId());
        }

        // 点赞数从 0 开始，避免前端传个乱七八糟的值进来
        comment.setLiked(0);

        boolean saved = save(comment);
        if (!saved) {
            return Result.fail("评论失败，请重试");
        }

        // 笔记上的评论数 +1
        postService.update()
                .setSql("comments = comments + 1")
                .eq("id", comment.getPostId())
                .update();

        return Result.ok(comment.getId());
    }

    /**
     * 分页查询某篇笔记的评论，按发表时间倒序。
     *
     * <p>两步查询：先分页取出一级评论，再把这一页评论下的所有回复一次性查出来，
     * 避免每查一条一级评论就发一次 SQL。</p>
     *
     * @param postId  笔记id
     * @param current 页码，从 1 开始
     * @return 评论列表，一级评论排在前面，回复跟在其后
     */
    @Override
    public Result queryCommentOfPost(Long postId, Integer current) {
        // 1. 分页查一级评论
        Page<EventPostComment> page = query()
                .eq("post_id", postId)
                .eq("parent_id", 0)
                .orderByDesc("create_time")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));

        List<EventPostComment> comments = page.getRecords();
        if (comments.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }

        // 2. 把这批一级评论的 id 收集起来，一次查出它们下面的所有回复
        List<Long> parentIds = comments.stream().map(EventPostComment::getId).toList();
        List<EventPostComment> replies = query()
                .in("parent_id", parentIds)
                .orderByAsc("create_time")
                .list();

        // 3. 合并成一个列表：一级评论在前，它自己的回复紧随其后
        List<EventPostComment> all = new ArrayList<>(comments.size() + replies.size());
        for (EventPostComment comment : comments) {
            all.add(comment);
            for (EventPostComment reply : replies) {
                if (reply.getParentId().equals(comment.getId())) {
                    all.add(reply);
                }
            }
        }

        // 4. 补上评论者和被回复人的昵称头像
        fillUserInfo(all);

        return Result.ok(all);
    }

    /**
     * 删除评论。
     *
     * <p>只允许删自己发的。删一级评论时会把挂在它下面的回复一起删掉，
     * 并按实际删掉的条数扣减笔记上的评论数，避免统计数字对不上。</p>
     *
     * @param id 评论id
     * @return 成功返回 ok；评论不存在或不是本人发的返回失败提示
     */
    @Override
    @Transactional
    public Result deleteComment(Long id) {
        EventPostComment comment = getById(id);
        if (comment == null) {
            return Result.fail("评论不存在");
        }

        Long userId = UserHolder.getUser().getId();
        if (!userId.equals(comment.getUserId())) {
            return Result.fail("只能删除自己的评论");
        }

        // 一级评论要把下面的回复一并删掉；回复直接删自己
        int deleted = 1;
        if (comment.getParentId() == 0) {
            List<EventPostComment> replies = query().eq("parent_id", id).list();
            if (!replies.isEmpty()) {
                removeByIds(replies.stream().map(EventPostComment::getId).toList());
                deleted += replies.size();
            }
        }
        removeById(id);

        // 同步扣减笔记上的评论数
        postService.update()
                .setSql("comments = comments - " + deleted)
                .eq("id", comment.getPostId())
                .update();

        return Result.ok();
    }

    /**
     * 批量为评论补上评论者昵称、头像，以及被回复人的昵称。
     *
     * <p>先把用到的用户 id 全部收集去重，一次查库，避免逐条查询造成的 N+1。</p>
     *
     * @param comments 待补全的评论列表
     */
    private void fillUserInfo(List<EventPostComment> comments) {
        Set<Long> userIds = new HashSet<>();
        for (EventPostComment comment : comments) {
            userIds.add(comment.getUserId());
            // 回复才有被回复人
            if (comment.getAnswerId() != null && comment.getAnswerId() != 0) {
                EventPostComment answered = getById(comment.getAnswerId());
                if (answered != null) {
                    userIds.add(answered.getUserId());
                    // 先记下来，等会儿统一换昵称
                    comment.setAnswerUserId(answered.getUserId());
                }
            }
        }

        List<User> users = userService.listByIds(userIds);
        // id -> 用户，方便下面 O(1) 取值
        Map<Long, User> userMap = new HashMap<>(users.size());
        for (User user : users) {
            userMap.put(user.getId(), user);
        }

        for (EventPostComment comment : comments) {
            User author = userMap.get(comment.getUserId());
            if (author != null) {
                comment.setNickName(author.getNickName());
                comment.setIcon(author.getIcon());
            }
            User answered = comment.getAnswerUserId() == null ? null : userMap.get(comment.getAnswerUserId());
            if (answered != null) {
                comment.setAnswerName(answered.getNickName());
            }
        }
    }
}
