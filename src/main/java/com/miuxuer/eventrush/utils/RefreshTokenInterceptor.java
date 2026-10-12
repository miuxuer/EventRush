package com.miuxuer.eventrush.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.miuxuer.eventrush.dto.UserDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Token 刷新拦截器，配置在所有请求上，负责还原并续期登录态。
 * <p>
 * 流程：从请求头 authorization 中取出 token → 用 token 拼出 Redis key 查询用户 → 查到则写入
 * ThreadLocal 并刷新 token 有效期（相当于滑动过期）。即使请求没带 token，也会直接放行，
 * 真正的"必须登录"拦截交给 {@link LoginInterceptor}。
 * </p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
public class RefreshTokenInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 构造方法注入 Redis 客户端。
     *
     * @param stringRedisTemplate 用于查询与续期登录 token 的 Redis 客户端
     */
    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 在进入 Controller 之前根据 token 还原登录用户并续期。
     *
     * @param request  当前 HTTP 请求，token 取自请求头 authorization
     * @param response 当前 HTTP 响应
     * @param handler  被调用的处理器
     * @return 始终返回 true，是否真正拦截由后续的 {@link LoginInterceptor} 决定
     * @throws Exception 处理过程中可能抛出的异常
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        /*//获取session
        HttpSession session = request.getSession();*/
        //从请求头中获取token
        String token = request.getHeader("authorization");
        //有token才去查redis；没token直接放行，真正的拦截交给 LoginInterceptor
        if (StrUtil.isNotBlank(token)){

            /*//获取session中的用户
            Object user = session.getAttribute("user");*/

            //基于token，获取到redis中的用户
            String key = RedisConstants.LOGIN_USER_KEY + token;
            Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(key);

            //判断用户是否存在
            if (!userMap.isEmpty()){
                //将查到的hash数据转换成dto对象
                UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);

                //保存用户信息到thread local
                UserHolder.saveUser(userDTO);

                //刷新token有效期
                stringRedisTemplate.expire(key, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
            }
        }

        return true;
    }

    /**
     * 请求处理完成后清理 ThreadLocal，避免 Tomcat 复用线程时把上一个请求的用户带进来。
     *
     * @param request  当前 HTTP 请求
     * @param response 当前 HTTP 响应
     * @param handler  被调用的处理器
     * @param ex       请求处理过程中抛出的异常，未抛出时为 null
     * @throws Exception 处理过程中可能抛出的异常
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
