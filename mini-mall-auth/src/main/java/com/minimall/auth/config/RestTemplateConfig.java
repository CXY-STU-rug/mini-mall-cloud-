package com.minimall.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate Bean (从 user 服务搬过来, 用来调 GitHub HTTP)
 *
 * 注意: 这里【没加】@LoadBalanced.
 *   - @LoadBalanced 让 RestTemplate 走 Nacos 负载均衡 (调内部服务用)
 *   - 调 GitHub 这种外部 URL 用普通 RestTemplate 就行, 不走 Nacos
 *
 * 服务间调用 (auth → user) 我们用 Feign, 不用 RestTemplate.
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
