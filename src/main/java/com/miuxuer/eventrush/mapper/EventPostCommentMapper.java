package com.miuxuer.eventrush.mapper;

import com.miuxuer.eventrush.entity.EventPostComment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 观演笔记评论数据访问接口，对应实体 {@link EventPostComment} 和数据库表 tb_event_post_comment。
 * <p>
 * 继承 MyBatis-Plus 的 BaseMapper，一级评论和二级回复都存放在同一张表，按 parentId、answerId 区分层级。
 * </p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
public interface EventPostCommentMapper extends BaseMapper<EventPostComment> {

}
