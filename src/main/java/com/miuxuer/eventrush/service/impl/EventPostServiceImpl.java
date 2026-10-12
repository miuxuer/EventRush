package com.miuxuer.eventrush.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.miuxuer.eventrush.dto.Result;
import com.miuxuer.eventrush.dto.ScrollResult;
import com.miuxuer.eventrush.dto.UserDTO;
import com.miuxuer.eventrush.entity.EventPost;
import com.miuxuer.eventrush.entity.Follow;
import com.miuxuer.eventrush.entity.User;
import com.miuxuer.eventrush.mapper.EventPostMapper;
import com.miuxuer.eventrush.service.IEventPostService;
import com.miuxuer.eventrush.service.IFollowService;
import com.miuxuer.eventrush.service.IUserService;
import com.miuxuer.eventrush.utils.SystemConstants;
import com.miuxuer.eventrush.utils.UserHolder;
import jakarta.annotation.Resource;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.miuxuer.eventrush.utils.RedisConstants.FEED_KEY;
import static com.miuxuer.eventrush.utils.RedisConstants.POST_LIKED_KEY;

/**
 * 观演笔记服务实现。
 *
 * <p>围绕笔记有三块功能：点赞、发布、以及关注流（收件箱）。</p>
 *
 * <p>点赞用 Redis 的 ZSet 存「谁在什么时候点的赞」，member 是用户 id、score 是时间戳。
 * 用 ZSet 而不是 Set，是为了能按点赞时间取出前几名点赞用户展示。</p>
 *
 * <p>关注流用的是「推模式」：作者一发笔记，就遍历他的粉丝，把笔记 id 写进每个粉丝的收件箱 ZSet 里。
 * 粉丝翻自己的关注流时直接从 ZSet 里读，不用去关联查询所有关注的人。</p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
@Service
public class EventPostServiceImpl extends ServiceImpl<EventPostMapper, EventPost> implements IEventPostService {

    @Resource
    private IUserService userService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private IFollowService followService;

    /**
     * 查询热门笔记，按点赞数倒序分页。
     *
     * @param current 页码，从 1 开始
     * @return 笔记列表，每一条都补齐了作者昵称、头像以及当前用户是否点过赞
     */
    @Override
    public Result queryHotPost(Integer current) {
        Page<EventPost> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        List<EventPost> records = page.getRecords();
        // 补齐作者信息和点赞状态
        records.forEach(post -> {
            this.queryPostUser(post);
            this.isPostLiked(post);
        });
        return Result.ok(records);
    }

    /**
     * 查询笔记详情。
     *
     * @param id 笔记id
     * @return 笔记详情；不存在时返回失败提示
     */
    @Override
    public Result queryPostById(Long id) {
        EventPost post = getById(id);
        if (post == null) {
            return Result.fail("笔记不存在！");
        }
        // 补齐作者信息和点赞状态
        queryPostUser(post);
        isPostLiked(post);
        return Result.ok(post);
    }

    /**
     * 判断当前登录用户是否给这条笔记点过赞，结果写回实体的 isLike 字段。
     *
     * <p>未登录时直接跳过 —— 游客也需要能看笔记，只是不显示点赞状态。</p>
     *
     * @param post 待判断的笔记
     */
    private void isPostLiked(EventPost post) {
        // 未登录不用查点赞状态
        UserDTO userDTO = UserHolder.getUser();
        if (userDTO == null) {
            return;
        }
        Long userId = userDTO.getId();
        String key = POST_LIKED_KEY + post.getId();

        // ZSet 里有这个用户，说明点过赞
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        post.setIsLike(score != null);
    }

    /**
     * 点赞 / 取消点赞（同一个接口，已赞则取消）。
     *
     * <p>数据库里的点赞数只在状态真的发生变化时才增减，避免重复点击把数字刷歪。</p>
     *
     * @param id 笔记id
     * @return 固定返回 ok
     */
    @Override
    public Result likePost(Long id) {
        Long userId = UserHolder.getUser().getId();
        String key = POST_LIKED_KEY + id;

        // 先看 ZSet 里有没有当前用户
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        if (score == null) {
            // 没点过赞，点赞：数据库点赞数 +1，同时把用户写进 ZSet（score 用当前时间戳）
            boolean isSuccess = update().setSql("liked = liked + 1").eq("id", id).update();
            if (isSuccess) {
                stringRedisTemplate.opsForZSet().add(key, userId.toString(), System.currentTimeMillis());
            }
        } else {
            // 已经点过赞，取消点赞：数据库点赞数 -1，同时把用户从 ZSet 移除
            boolean isSuccess = update().setSql("liked = liked - 1").eq("id", id).update();
            if (isSuccess) {
                stringRedisTemplate.opsForZSet().remove(key, userId.toString());
            }
        }
        return Result.ok();
    }

    /**
     * 查询给这条笔记点赞的前 5 个用户。
     *
     * <p>ZSet 的 score 是点赞时间戳，按 score 正序取前 5 个就是最早点赞的那批人。</p>
     *
     * @param id 笔记id
     * @return 点赞用户列表（只含 id、昵称、头像），没人点赞时返回空列表
     */
    @Override
    public Result queryPostLikes(Long id) {
        // 查询top5的点赞用户
        String key = POST_LIKED_KEY + id;
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        if (top5 == null) {
            return Result.ok(Collections.emptyList());
        }

        List<Long> ids = top5.stream().map(Long::valueOf).toList();
        // 没有人点赞，直接返回空列表，不用再查数据库
        if (CollUtil.isEmpty(ids)) {
            return Result.ok(Collections.emptyList());
        }

        // 按 ZSet 里的顺序取用户信息，order by field 保证返回顺序和 Redis 里一致
        String idStr = StrUtil.join(",", ids);
        List<UserDTO> userDTOList = userService
                .query().in("id", ids).last("order by field(id," + idStr + ")").list()
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .toList();

        return Result.ok(userDTOList);
    }

    /**
     * 发布笔记，并把笔记推送到所有粉丝的收件箱。
     *
     * <p>推送失败的粉丝不必回滚笔记本身，所以这里没有加事务：笔记先落库，
     * 再挨个往粉丝的收件箱 ZSet 里塞一条记录。</p>
     *
     * @param post 笔记内容
     * @return 成功时 data 为笔记 id；保存失败返回失败提示
     */
    @Override
    public Result savePost(EventPost post) {
        // 作者就是当前登录用户，前端不用传
        Long userId = UserHolder.getUser().getId();
        post.setUserId(userId);

        boolean isSuccess = save(post);
        if (!isSuccess) {
            return Result.fail("新增笔记失败！");
        }

        // 查出作者的所有粉丝，把笔记 id 推进每个人的收件箱
        List<Follow> follows = followService.query().eq("follow_user_id", userId).list();
        for (Follow follow : follows) {
            // 这里取的是粉丝自己的 id
            Long fanId = follow.getUserId();
            String key = FEED_KEY + fanId;
            stringRedisTemplate.opsForZSet().add(key, post.getId().toString(), System.currentTimeMillis());
        }

        return Result.ok(post.getId());
    }

    /**
     * 滚动分页查询关注流（当前登录用户关注的作者发的笔记）。
     *
     * <p>收件箱是 ZSet，score 是笔记发布的时间戳。用「最后一个 score + 偏移量」当游标，
     * 而不是用页码：因为一边翻页一边有新笔记进来时，页码分页会导致重复或漏掉数据。</p>
     *
     * @param max    上一页最后一条笔记的时间戳，第一页传当前时间戳
     * @param offset 和 max 时间戳相同的记录里，要跳过几条（同一毫秒可能有多条）
     * @return {@link ScrollResult}，其中的 minTime 和 offset 要给下一次请求带回来
     */
    @Override
    public Result queryPostOfFollow(Long max, Integer offset) {
        Long userId = UserHolder.getUser().getId();
        // 查询收件箱
        String key = FEED_KEY + userId;
        Set<ZSetOperations.@NonNull TypedTuple<String>> typedTuples =
                stringRedisTemplate.opsForZSet().reverseRangeByScoreWithScores(key, 0, max, offset, 3);

        if (typedTuples == null || typedTuples.isEmpty()) {
            return Result.ok();
        }

        // 解析出笔记 id，并算出下一次分页的游标
        List<Long> ids = new ArrayList<>(typedTuples.size());
        long minTime = 0;
        int os = 1;
        for (ZSetOperations.TypedTuple<String> tuple : typedTuples) {
            ids.add(Long.valueOf(tuple.getValue()));
            // 循环结束后 minTime 就是最后一条（也就是最小）的时间戳
            long time = tuple.getScore().longValue();
            if (time == minTime) {
                // 时间戳和上一条相同，说明这一毫秒有多条，偏移量要累加
                os++;
            } else {
                minTime = time;
                os = 1;
            }
        }

        // 根据 id 查询笔记，order by field 保证顺序和收件箱一致
        String idStr = StrUtil.join(",", ids);
        List<EventPost> posts = query().in("id", ids).last("order by field(id," + idStr + ")").list();
        for (EventPost post : posts) {
            // 补齐作者信息和点赞状态
            queryPostUser(post);
            isPostLiked(post);
        }

        ScrollResult scrollResult = ScrollResult.builder().list(posts).minTime(minTime).offset(os).build();
        return Result.ok(scrollResult);
    }

    /**
     * 把笔记作者的昵称和头像补到笔记对象上。
     *
     * @param post 待补全的笔记
     */
    private void queryPostUser(EventPost post) {
        Long userId = post.getUserId();
        User user = userService.getById(userId);
        post.setName(user.getNickName());
        post.setIcon(user.getIcon());
    }
}
