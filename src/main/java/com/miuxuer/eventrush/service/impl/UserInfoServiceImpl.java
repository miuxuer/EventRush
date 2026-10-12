package com.miuxuer.eventrush.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.miuxuer.eventrush.entity.UserInfo;
import com.miuxuer.eventrush.mapper.UserInfoMapper;
import com.miuxuer.eventrush.service.IUserInfoService;
import org.springframework.stereotype.Service;

/**
 * 用户详情服务实现。
 *
 * <p>用户详情（城市、简介、粉丝数、积分等）目前只用到查询，直接复用基础 CRUD。</p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {

}
