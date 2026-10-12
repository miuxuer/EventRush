package com.miuxuer.eventrush.mapper;

import com.miuxuer.eventrush.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 用户账号数据访问接口，对应实体 {@link User} 和数据库表 tb_user。
 * <p>
 * 继承 MyBatis-Plus 的 BaseMapper；登录时按手机号查询用户直接使用其内置的条件构造方法。
 * </p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
public interface UserMapper extends BaseMapper<User> {

}
