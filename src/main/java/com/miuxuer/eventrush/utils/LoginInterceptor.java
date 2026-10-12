package com.miuxuer.eventrush.utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录校验拦截器，负责拦住"未登录"的请求。
 * <p>
 * 它本身不解析 token，只看 ThreadLocal 中是否有用户 —— 用户信息的还原和续期已由前置的
 * {@link RefreshTokenInterceptor} 完成，这里只做最后一道拦截判断，因此它只配在需要登录的路径上。
 * </p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
public class LoginInterceptor implements HandlerInterceptor {

    /**
     * 在进入 Controller 之前判断当前请求是否已登录。
     *
     * @param request  当前 HTTP 请求
     * @param response 当前 HTTP 响应，未登录时写入 401 状态码
     * @param handler  被调用的处理器
     * @return true：已登录，放行；false：未登录，直接拦截
     * @throws Exception 处理过程中可能抛出的异常
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //判断是否需要拦截（thread local里是否有用户数据）
        if (UserHolder.getUser() == null) {
            //拦截，返回401状态码
            response.setStatus(401);
            return false;
        }
        //有用户数据
        return true;
    }
}
