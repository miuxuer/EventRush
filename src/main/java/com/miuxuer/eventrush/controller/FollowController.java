package com.miuxuer.eventrush.controller;

import com.miuxuer.eventrush.dto.Result;
import com.miuxuer.eventrush.service.IFollowService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 关注接口。
 *
 * <p>这些接口都要求登录，游客不能关注别人。</p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
@RestController
@RequestMapping("/follow")
public class FollowController {

    @Resource
    private IFollowService followService;

    /**
     * 关注或取关某个用户。
     *
     * @param followUserId 被关注/取关的用户id
     * @param isFollow     true 表示关注，false 表示取关
     * @return 无
     */
    @PutMapping("/{id}/{isFollow}")
    public Result follow(@PathVariable("id") Long followUserId, @PathVariable("isFollow") Boolean isFollow) {
        return followService.follow(followUserId, isFollow);
    }

    /**
     * 查询当前用户是否关注了某个用户。
     *
     * @param followUserId 目标用户id
     * @return data 为 true / false
     */
    @GetMapping("/or/not/{id}")
    public Result isFollow(@PathVariable("id") Long followUserId) {
        return followService.isFollow(followUserId);
    }

    /**
     * 查询当前用户和某个用户的共同关注。
     *
     * <p>用来在别人的主页上显示「你们都关注了谁」。</p>
     *
     * @param id 目标用户id
     * @return 共同关注的用户列表
     */
    @GetMapping("/common/{id}")
    public Result followCommons(@PathVariable("id") Long id) {
        return followService.followCommons(id);
    }
}
