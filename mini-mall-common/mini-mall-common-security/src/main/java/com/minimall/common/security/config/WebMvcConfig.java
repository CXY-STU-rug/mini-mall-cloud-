package com.minimall.common.security.config;

import com.minimall.common.security.interceptor.HeaderInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 把 HeaderInterceptor 注册进 Spring MVC, 让所有请求都过它一遍
 *
 * WebMvcConfigurer 是 Spring 提供的"配置 MVC"标准入口接口.
 * Spring 启动时找到所有 WebMvcConfigurer Bean, 依次调它们的 addInterceptors 收集.
 *
 * ⭐ 必须独立 @AutoConfiguration + 走 imports 文件注册 (而不是被 SecurityAutoConfiguration @Import 引):
 *   如果被 @Import 直接引用, JVM 加载父配置类时强制加载本类 → 触发 implements WebMvcConfigurer →
 *   gateway (WebFlux, 没 WebMvcConfigurer 类) → NoClassDefFoundError 启不来.
 *
 *   走 imports 文件, Spring 先用【ASM 字节码扫描】 检查 @ConditionalOnClass:
 *     - 类不存在 → 直接跳过, 不加载本类, gateway 就安全
 *     - 类存在  → 才真正加载并实例化
 *
 * @ConditionalOnClass(name="字符串") 关键: name 形式让 Spring 走 ASM 路径,
 *   value=class 字面量形式反而会触发类加载, 在此场景下绝对不能用.
 */
@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.web.servlet.config.annotation.WebMvcConfigurer")
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 HeaderInterceptor; 默认拦所有路径
        // .addPathPatterns("/**") 可省略, 不设就是全部
        registry.addInterceptor(new HeaderInterceptor());
    }
}
