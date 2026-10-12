package com.miuxuer.eventrush.config;

import com.miuxuer.eventrush.utils.LoginInterceptor;
import com.miuxuer.eventrush.utils.RefreshTokenInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 配置，主要用来注册两个登录相关的拦截器。
 *
 * <p>两个拦截器分工不同、顺序也不同：</p>
 * <ol>
 *   <li>{@link RefreshTokenInterceptor}（order = 0，先执行）—— 拦截全部请求，
 *       发现有 token 就去 Redis 查用户、写进 ThreadLocal 并给登录态续期。
 *       没 token 也放行，真正的拦截交给下一个拦截器。</li>
 *   <li>{@link LoginInterceptor}（order = 1，后执行）—— 只检查 ThreadLocal 里有没有用户，
 *       没有就返回 401。写在下面的白名单里的接口不会被它拦。</li>
 * </ol>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 注册拦截器。
     *
     * @param registry 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // order() 里的值越大，优先级越低
        // 登录拦截器：白名单里的接口游客也能访问
        registry.addInterceptor(new LoginInterceptor())
                .excludePathPatterns(
                        "/event/**",
                        "/ticket/**",
                        "/category/**",
                        "/upload/**",
                        "/post/hot",
                        "/post/of/user",
                        "/post/likes/**",
                        "/post-comments/of/**",
                        "/user/code",
                        "/user/login"
                ).order(1);

        // token 刷新拦截器，拦截所有请求，负责把登录态还原到 ThreadLocal
        registry.addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate))
                .excludePathPatterns()
                .order(0);
    }
}
