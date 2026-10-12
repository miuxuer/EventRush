package com.miuxuer.eventrush.utils;

import com.miuxuer.eventrush.dto.UserDTO;

/**
 * 基于 ThreadLocal 的当前登录用户容器。
 * <p>
 * 请求进入时由 {@link RefreshTokenInterceptor} 校验 token 后调用 saveUser 存入用户，
 * 之后同一个请求线程内的任意代码都能通过 getUser 直接拿到用户，无需层层传参；
 * 请求结束时必须在 afterCompletion 中调用 removeUser，避免线程复用导致数据串号或内存泄漏。
 * </p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
public class UserHolder {

    /**
     * 每个请求线程独立持有的用户信息
     */
    private static final ThreadLocal<UserDTO> tl = new ThreadLocal<>();

    /**
     * 把当前登录用户保存到本线程的 ThreadLocal 中。
     *
     * @param user 当前登录用户
     */
    public static void saveUser(UserDTO user){
        tl.set(user);
    }

    /**
     * 获取当前线程中保存的登录用户。
     *
     * @return 当前登录用户；未登录时返回 null
     */
    public static UserDTO getUser(){
        return tl.get();
    }

    /**
     * 清除本线程保存的用户信息，请求处理结束后调用，防止线程复用造成串号。
     */
    public static void removeUser(){
        tl.remove();
    }
}
