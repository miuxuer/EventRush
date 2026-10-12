package com.miuxuer.eventrush.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.miuxuer.eventrush.dto.Result;
import com.miuxuer.eventrush.dto.UserDTO;
import com.miuxuer.eventrush.entity.Follow;
import com.miuxuer.eventrush.mapper.FollowMapper;
import com.miuxuer.eventrush.service.IFollowService;
import com.miuxuer.eventrush.service.IUserService;
import com.miuxuer.eventrush.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 关注服务实现。
 *
 * <p>关注关系存两份：</p>
 * <ul>
 *   <li>数据库 tb_follow —— 权威数据，保证不会丢；</li>
 *   <li>Redis 的 Set（key 为 {@code follows:用户id}）—— 只是为了能高效地求两个人的「共同关注」，
 *       用 Set 的交集命令一次算完，不用去数据库里做两次关联查询。</li>
 * </ul>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private IUserService userService;

    /**
     * 关注或取关某个用户。
     *
     * <p>数据库和 Redis 一起改：关注时插入一条记录并把自己关注的用户写进 Redis 的 Set；
     * 取关时删记录并从 Set 里移除。</p>
     *
     * @param followUserId 被关注/取关的用户id
     * @param isFollow     true 表示关注，false 表示取关
     * @return 固定返回 ok
     */
    @Override
    public Result follow(Long followUserId, Boolean isFollow) {
        // 当前登录用户，也就是「谁在关注别人」
        Long userId = UserHolder.getUser().getId();
        // 每个用户一个 Set，存他关注了谁
        String key = "follows:" + userId;

        if (isFollow) {
            // 关注：先落库
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(followUserId);
            boolean isSuccess = save(follow);
            if (isSuccess) {
                // 落库成功再把被关注的用户 id 写进 Redis
                stringRedisTemplate.opsForSet().add(key, followUserId.toString());
            }
        } else {
            // 取关：删掉这条关注记录
            boolean isSuccess = remove(new QueryWrapper<Follow>()
                    .eq("user_id", userId)
                    .eq("follow_user_id", followUserId));
            if (isSuccess) {
                // 同步把 Redis 里的记录移除
                stringRedisTemplate.opsForSet().remove(key, followUserId.toString());
            }
        }
        return Result.ok();
    }

    /**
     * 查询当前登录用户是否关注了某个用户。
     *
     * <p>直接查数据库的 count，因为这条数据要求准确，不适合走缓存。</p>
     *
     * @param followUserId 目标用户id
     * @return data 为 true / false
     */
    @Override
    public Result isFollow(Long followUserId) {
        Long userId = UserHolder.getUser().getId();
        Long count = query().eq("user_id", userId).eq("follow_user_id", followUserId).count();
        return Result.ok(count > 0);
    }

    /**
     * 查询当前登录用户和某个用户的共同关注。
     *
     * <p>两个人的关注列表各是一个 Redis Set，直接求交集，再把交出来的用户 id 换成用户信息返回。</p>
     *
     * @param id 目标用户id
     * @return 共同关注的用户列表（只含 id、昵称、头像），没有共同关注时返回空列表
     */
    @Override
    public Result followCommons(Long id) {
        Long userId = UserHolder.getUser().getId();
        // 当前用户的关注集合
        String key = "follows:" + userId;
        // 目标用户的关注集合
        String key2 = "follows:" + id;

        // 求交集
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(key, key2);
        if (intersect == null || intersect.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }

        // 把交出来的 id 转成用户信息返回
        List<Long> userIds = intersect.stream().map(Long::valueOf).toList();
        List<UserDTO> userDTOList = userService.listByIds(userIds)
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .toList();
        return Result.ok(userDTOList);
    }
}
