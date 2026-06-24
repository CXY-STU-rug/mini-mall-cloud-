package com.minimall.common.security.config;

import com.minimall.common.security.interceptor.FeignAuthInterceptor;
import com.minimall.common.security.properties.JwtProperties;
import com.minimall.common.security.util.JwtUtil;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * common-security 自动装配总入口
 *
 * 业务服务引依赖 + Spring Boot 启动 → 通过 META-INF/.../AutoConfiguration.imports 找到本类
 *
 * 本类一次性激活的事:
 *   ① @EnableConfigurationProperties(JwtProperties.class)
 *      → 让 JwtProperties 实例化, 字段绑定 yml jwt.*
 *
 *   ② @Import(JwtUtil.class)
 *      → JwtUtil 作为 Bean 进入容器 (虽然它有 @Component, 但业务模块默认不扫 common 包, 显式 Import 更稳)
 *
 *   ③ @Bean FeignAuthInterceptor
 *      → 注册 Feign 出站拦截器, Feign 启动自动收集所有 RequestInterceptor Bean
 *
 * ⭐ WebMvcConfig 改走独立 AutoConfiguration 走 imports 文件注册, 不在这里 @Import:
 *   如果在这里 @Import(WebMvcConfig.class), JVM 加载本类时就强制把 WebMvcConfig 拉进 ClassLoader,
 *   而 WebMvcConfig implements WebMvcConfigurer; gateway 是 WebFlux 没有 WebMvcConfigurer 类 → NoClassDefFoundError.
 *   走 imports 文件, Spring 用 ASM 字节码扫描先检查 @ConditionalOnClass, 条件不满足就根本不加载类.
 *
 * 为啥用 @AutoConfiguration 不用 @Configuration:
 *   @AutoConfiguration = Spring Boot 3 专门给"被 imports 文件加载的配置类"用的注解,
 *   继承 @Configuration + 加元数据让 Spring Boot 顺序更可控.
 */
@AutoConfiguration
@EnableConfigurationProperties(JwtProperties.class)
@Import(JwtUtil.class)
public class SecurityAutoConfiguration {

    /**
     * Feign 出站拦截器, 业务服务只要有 OpenFeign, 这个 Bean 就自动生效
     */
    @Bean
    public FeignAuthInterceptor feignAuthInterceptor() {
        return new FeignAuthInterceptor();
    }
}
