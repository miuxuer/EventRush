package com.miuxuer.eventrush.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.miuxuer.eventrush.dto.LoginFormDTO;
import com.miuxuer.eventrush.dto.Result;
import com.miuxuer.eventrush.entity.User;
import jakarta.servlet.http.HttpSession;

/**
 * 用户服务。
 *
 * @author miuxuer
 * @since 2026-10-12
 */
public interface IUserService extends IService<User> {

    /**
     * 发送短信验证码。
     *
     * <p>验证码写进 Redis，有效期两分钟。这里没有真的对接短信平台，验证码直接打在日志里。</p>
     *
     * @param phone   手机号
     * @param session 当前会话，登录态已经改用 Redis 存，这个参数目前只是保留
     * @return 无
     */
    Result sendCode(String phone, HttpSession session);

    /**
     * 手机号 + 验证码登录，用户不存在时自动注册。
     *
     * <p>登录成功后会生成一个 token 并把用户信息存进 Redis 的 Hash，token 返回给前端，
     * 后续请求带上它就能通过登录校验。</p>
     *
     * @param loginForm 登录表单，含手机号和验证码
     * @param session   当前会话，同上
     * @return 成功时 data 为 token
     */
    Result login(LoginFormDTO loginForm, HttpSession session);

    /**
     * 退出登录：把 token 对应的登录态从 Redis 里删掉。
     *
     * @param token 请求头里带的登录令牌
     * @return 无
     */
    Result logout(String token);

    /**
     * 用户签到。
     *
     * <p>用 BitMap 记录，key 是「用户 + 年月」，第几天就写第几个 bit，一个月只占 4 个字节。</p>
     *
     * @return 无
     */
    Result sign();

    /**
     * 统计当前用户本月截至今天已经连续签到多少天。
     *
     * @return data 为连续签到天数
     */
    Result signCount();
}
