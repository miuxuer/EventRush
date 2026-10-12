package com.miuxuer.eventrush.mapper;

import com.miuxuer.eventrush.entity.Follow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 关注关系数据访问接口，对应实体 {@link Follow} 和数据库表 tb_follow。
 * <p>
 * 继承 MyBatis-Plus 的 BaseMapper，关注/取关时对 tb_follow 的增删直接使用内置方法。
 * </p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
public interface FollowMapper extends BaseMapper<Follow> {

}
