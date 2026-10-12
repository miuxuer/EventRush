package com.miuxuer.eventrush.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.miuxuer.eventrush.dto.Result;
import com.miuxuer.eventrush.entity.Follow;

/**
 * 关注服务。
 *
 * <p>关注关系同时写数据库和 Redis 的 Set：数据库保证不丢，Redis 用来快速算「共同关注」。</p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
public interface IFollowService extends IService<Follow> {

    /**
     * 关注或取关某个用户。
     *
     * @param followUserId 被关注/取关的用户id
     * @param isFollow     true 表示关注，false 表示取关
     * @return 无
     */
    Result follow(Long followUserId, Boolean isFollow);

    /**
     * 查询当前登录用户是否关注了某个用户。
     *
     * @param followUserId 目标用户id
     * @return data 为 true / false
     */
    Result isFollow(Long followUserId);

    /**
     * 查询当前登录用户和某个用户的共同关注。
     *
     * <p>两个用户的关注列表在 Redis 里各是一个 Set，求交集即可。</p>
     *
     * @param id 目标用户id
     * @return 共同关注的用户列表
     */
    Result followCommons(Long id);
}
