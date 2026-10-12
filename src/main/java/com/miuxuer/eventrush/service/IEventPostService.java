package com.miuxuer.eventrush.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.miuxuer.eventrush.dto.Result;
import com.miuxuer.eventrush.entity.EventPost;

/**
 * 观演笔记服务。
 *
 * @author miuxuer
 * @since 2026-10-12
 */
public interface IEventPostService extends IService<EventPost> {

    /**
     * 查询热门笔记，按点赞数倒序。
     *
     * @param current 页码
     * @return 笔记列表
     */
    Result queryHotPost(Integer current);

    /**
     * 查询笔记详情。
     *
     * @param id 笔记id
     * @return 笔记详情
     */
    Result queryPostById(Long id);

    /**
     * 点赞或取消点赞。
     *
     * @param id 笔记id
     * @return 无
     */
    Result likePost(Long id);

    /**
     * 查询给笔记点赞的前 5 个用户。
     *
     * @param id 笔记id
     * @return 用户列表
     */
    Result queryPostLikes(Long id);

    /**
     * 发布笔记，并推到所有粉丝的收件箱。
     *
     * @param post 笔记内容
     * @return 新笔记的 id
     */
    Result savePost(EventPost post);

    /**
     * 滚动分页查询关注流。
     *
     * @param max    上一页最后一条的时间戳
     * @param offset 同时间戳下的偏移量
     * @return 笔记列表和下一页的游标
     */
    Result queryPostOfFollow(Long max, Integer offset);
}
