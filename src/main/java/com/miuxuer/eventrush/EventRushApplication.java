package com.miuxuer.eventrush;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * EventRush 赛事演出抢票平台启动类。
 *
 * @author miuxuer
 * @since 2026-10-12
 */
// exposeProxy = true 必须加：AopContext.currentProxy() 靠它才能拿到当前代理对象
@EnableAspectJAutoProxy(exposeProxy = true, proxyTargetClass = true)
@MapperScan("com.miuxuer.eventrush.mapper")
@SpringBootApplication
public class EventRushApplication {

    /**
     * 应用入口。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(EventRushApplication.class, args);
    }

}
