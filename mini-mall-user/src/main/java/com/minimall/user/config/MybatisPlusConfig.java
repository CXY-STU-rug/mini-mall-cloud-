package com.minimall.user.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 分页插件配置 (ADMIN.3 阶段加)
 *
 * 不配这个, mapper.selectPage / IService.page() 不会真的 LIMIT,
 * 会把整张表拉出来再内存分页, 几万行就崩.
 *
 * PaginationInnerInterceptor:
 *   ① 拦截带 Page 参数的查询
 *   ② 自动加 LIMIT offset, size
 *   ③ 自动跑一次 SELECT COUNT(*) 拿总数
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
