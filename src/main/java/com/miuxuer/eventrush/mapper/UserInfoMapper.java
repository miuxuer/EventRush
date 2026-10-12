package com.miuxuer.eventrush.mapper;

import com.miuxuer.eventrush.entity.UserInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 用户详情数据访问接口，对应实体 {@link UserInfo} 和数据库表 tb_user_info。
 * <p>
 * 继承 MyBatis-Plus 的 BaseMapper，用户资料的读写直接使用其内置方法。
 * </p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
public interface UserInfoMapper extends BaseMapper<UserInfo> {

}
