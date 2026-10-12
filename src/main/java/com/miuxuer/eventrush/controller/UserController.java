package com.miuxuer.eventrush.controller;

import cn.hutool.core.bean.BeanUtil;
import com.miuxuer.eventrush.dto.LoginFormDTO;
import com.miuxuer.eventrush.dto.Result;
import com.miuxuer.eventrush.dto.UserDTO;
import com.miuxuer.eventrush.entity.User;
import com.miuxuer.eventrush.entity.UserInfo;
import com.miuxuer.eventrush.service.IUserInfoService;
import com.miuxuer.eventrush.service.IUserService;
import com.miuxuer.eventrush.utils.UserHolder;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口：验证码、登录登出、个人信息、签到。
 *
 * @author miuxuer
 * @since 2026-10-12
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private IUserService userService;

    @Resource
    private IUserInfoService userInfoService;

    /**
     * 发送手机验证码。
     *
     * @param phone   手机号
     * @param session 当前会话，登录态已改用 Redis，这里保留参数只是为了兼容调用方
     * @return 无（验证码打在应用日志里）
     */
    @PostMapping("code")
    public Result sendCode(@RequestParam("phone") String phone, HttpSession session) {
        return userService.sendCode(phone, session);
    }

    /**
     * 登录，用户不存在时自动注册。
     *
     * @param loginForm 登录参数，含手机号和验证码
     * @param session   当前会话，同上
     * @return 成功时 data 为登录令牌 token，后续请求要放在 authorization 请求头里
     */
    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm, HttpSession session) {
        return userService.login(loginForm, session);
    }

    /**
     * 退出登录。
     *
     * <p>登录态存在 Redis 里，服务端把 token 对应的记录删掉即完成登出，
     * 前端同时把本地保存的 token 清除即可。</p>
     *
     * @param token 请求头 authorization 里的登录令牌
     * @return 无
     */
    @PostMapping("/logout")
    public Result logout(@RequestHeader(value = "authorization", required = false) String token) {
        return userService.logout(token);
    }

    /**
     * 获取当前登录用户。
     *
     * <p>用户信息是上游拦截器解析 token 后放进 ThreadLocal 的，这里直接取就行。</p>
     *
     * @return 当前登录用户的 id、昵称、头像
     */
    @GetMapping("/me")
    public Result me() {
        UserDTO user = UserHolder.getUser();
        return Result.ok(user);
    }

    /**
     * 查询用户详情（城市、简介、粉丝数、积分等）。
     *
     * @param userId 用户id
     * @return 用户详情；没有详情记录时返回空
     */
    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId) {
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            // 没有详情，应该是第一次查看详情
            return Result.ok();
        }
        // 创建和更新时间对前端没意义，不返回
        info.setCreateTime(null);
        info.setUpdateTime(null);
        return Result.ok(info);
    }

    /**
     * 按 id 查询用户的基本信息。
     *
     * @param userId 用户id
     * @return 用户的 id、昵称、头像；用户不存在时返回空
     */
    @GetMapping("/{id}")
    public Result queryUserById(@PathVariable("id") Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            return Result.ok();
        }
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        return Result.ok(userDTO);
    }

    /**
     * 用户签到。
     *
     * @return 无
     */
    @PostMapping("/sign")
    public Result sign() {
        return userService.sign();
    }

    /**
     * 查询本月截至今天已连续签到多少天。
     *
     * @return data 为连续签到天数
     */
    @GetMapping("/sign/count")
    public Result signCount() {
        return userService.signCount();
    }
}
