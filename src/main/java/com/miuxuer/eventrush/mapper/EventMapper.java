package com.miuxuer.eventrush.mapper;

import com.miuxuer.eventrush.entity.Event;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 演出数据访问接口，对应实体 {@link Event} 和数据库表 tb_event。
 * <p>
 * 继承 MyBatis-Plus 的 BaseMapper，普通的单表增删改查直接复用其内置方法；
 * 演出的分类分页查询、按距离远近搜索等逻辑在 Service 层借助 Redis 实现，无需在此额外定义。
 * </p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
public interface EventMapper extends BaseMapper<Event> {

}
