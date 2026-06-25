package com.minimall.order.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 分页插件 (ADMIN.5 新增)
 *
 * 没这个 Bean: ServiceImpl.page() 不会改写 SQL 加 LIMIT, 全表扫返回 +
 *              IPage.total 一直是 0
 *
 * 跟 user / product 服务的 MybatisPlusConfig 一模一样, 只是包名不同
 *
 * 历史: order 之前没用过分页 (用户端 /order/my 直接 List 返全部),
 *       所以一直没暴露这个缺口; ADMIN.5 后台分页才发现
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
