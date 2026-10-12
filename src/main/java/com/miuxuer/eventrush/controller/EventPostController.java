package com.miuxuer.eventrush.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.miuxuer.eventrush.dto.Result;
import com.miuxuer.eventrush.dto.UserDTO;
import com.miuxuer.eventrush.entity.EventPost;
import com.miuxuer.eventrush.service.IEventPostService;
import com.miuxuer.eventrush.utils.SystemConstants;
import com.miuxuer.eventrush.utils.UserHolder;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 观演笔记接口。
 *
 * <p>笔记就是用户看完演出后发的图文感受，是平台的内容社区部分。评论相关接口在
 * {@link EventPostCommentController} 里。</p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
@RestController
@RequestMapping("/post")
public class EventPostController {

    @Resource
    private IEventPostService postService;

    /**
     * 发布笔记。
     *
     * <p>发布后会推送到作者所有粉丝的关注流里。</p>
     *
     * @param post 笔记内容（标题、图片、正文）
     * @return 新笔记的 id
     */
    @PostMapping
    public Result savePost(@RequestBody EventPost post) {
        return postService.savePost(post);
    }

    /**
     * 点赞 / 取消点赞。
     *
     * <p>同一个接口，已经点过赞就是取消，未点赞就是点赞。</p>
     *
     * @param id 笔记id
     * @return 无
     */
    @PutMapping("/like/{id}")
    public Result likePost(@PathVariable("id") Long id) {
        return postService.likePost(id);
    }

    /**
     * 查询当前登录用户自己发的笔记。
     *
     * @param current 页码，默认第一页
     * @return 笔记列表
     */
    @GetMapping("/of/me")
    public Result queryMyPost(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        UserDTO user = UserHolder.getUser();
        Page<EventPost> page = postService.query()
                .eq("user_id", user.getId()).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        List<EventPost> records = page.getRecords();
        return Result.ok(records);
    }

    /**
     * 查询热门笔记，按点赞数倒序。
     *
     * @param current 页码，默认第一页
     * @return 笔记列表
     */
    @GetMapping("/hot")
    public Result queryHotPost(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        return postService.queryHotPost(current);
    }

    /**
     * 查询笔记详情。
     *
     * @param id 笔记id
     * @return 笔记详情，含作者信息和当前用户的点赞状态
     */
    @GetMapping("/{id}")
    public Result queryPostById(@PathVariable("id") Long id) {
        return postService.queryPostById(id);
    }

    /**
     * 查询给这条笔记点赞的前 5 个用户。
     *
     * @param id 笔记id
     * @return 用户列表
     */
    @GetMapping("/likes/{id}")
    public Result queryPostLikes(@PathVariable("id") Long id) {
        return postService.queryPostLikes(id);
    }

    /**
     * 查询指定用户发布的笔记（看别人主页时用）。
     *
     * @param current 页码，默认第一页
     * @param id      目标用户id
     * @return 笔记列表
     */
    @GetMapping("/of/user")
    public Result queryPostByUserId(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam("id") Long id) {
        Page<EventPost> page = postService.query()
                .eq("user_id", id).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        List<EventPost> records = page.getRecords();
        return Result.ok(records);
    }

    /**
     * 滚动分页查询关注流。
     *
     * <p>不用页码分页，而是用上一页返回的 minTime / offset 当游标 ——
     * 一边下拉一边有新笔记时，页码分页会出现重复或漏数据。</p>
     *
     * @param max    上一页最后一条笔记的时间戳，第一页传当前时间戳
     * @param offset 和 max 同一时间戳的记录里需要跳过几条，默认 0
     * @return 笔记列表 + 下一页要用的游标
     */
    @GetMapping("/of/follow")
    public Result queryPostOfFollow(@RequestParam("lastId") Long max,
                                    @RequestParam(value = "offset", defaultValue = "0") Integer offset) {
        return postService.queryPostOfFollow(max, offset);
    }
}
