package com.miuxuer.eventrush.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置。
 *
 * @author miuxuer
 * @since 2026-10-12
 */
@Configuration
public class MybatisConfig {

    /**
     * 注册分页插件。
     *
     * <p>不加这个插件，代码里用 {@code Page} 做的分页不会生效 —— 它会返回全部数据再在内存里截取。
     * 指定数据库类型是为了让插件按 MySQL 的语法生成 limit 语句。</p>
     *
     * @return MyBatis-Plus 拦截器链
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
