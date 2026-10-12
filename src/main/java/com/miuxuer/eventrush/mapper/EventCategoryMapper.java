package com.miuxuer.eventrush.mapper;

import com.miuxuer.eventrush.entity.EventCategory;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 演出分类数据访问接口，对应实体 {@link EventCategory} 和数据库表 tb_event_category。
 * <p>
 * 继承 MyBatis-Plus 的 BaseMapper，分类的增删改查直接使用其内置方法即可。
 * </p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
public interface EventCategoryMapper extends BaseMapper<EventCategory> {

}
