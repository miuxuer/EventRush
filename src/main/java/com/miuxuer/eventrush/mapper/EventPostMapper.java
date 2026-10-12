package com.miuxuer.eventrush.mapper;

import com.miuxuer.eventrush.entity.EventPost;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 观演笔记数据访问接口，对应实体 {@link EventPost} 和数据库表 tb_event_post。
 * <p>
 * 继承 MyBatis-Plus 的 BaseMapper；笔记的点赞量排序、Feed 流推送等逻辑在 Service 层结合 Redis 完成。
 * </p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
public interface EventPostMapper extends BaseMapper<EventPost> {

}
