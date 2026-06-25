package com.minimall.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 配置
 *
 * 作用: 给 Spring 容器塞一个 RestTemplate Bean, 业务代码可 @Autowired 拿来用.
 *
 * 为什么要自己声明:
 *   Spring Boot 不自动注册 RestTemplate (因为推荐用 WebClient/RestClient).
 *   但 RestTemplate API 简单, 教学场景够用.
 *
 * 后续微服务化时:
 *   如果想让 RestTemplate 也走 Nacos 负载均衡 (调内部服务),
 *   加 @LoadBalanced 注解. 但调 GitHub 这种外部 URL 不需要.
 */
@Configuration
public class RestTemplateConfig {

    /** 注册一个全局 RestTemplate, Controller / Service 都能 @Autowired 拿到 */
    @Bean
    public RestTemplate restTemplate() {
        // new 一个就完事, 默认配置够调 GitHub 用
        return new RestTemplate();
    }
}