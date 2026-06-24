package com.minimall.common.security.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置属性
 *
 * 把 application.yml 里 jwt.* 段的所有字段绑到本类:
 *   jwt:
 *     secret: your-256-bit-secret
 *     expiration: 7200000
 *
 * @ConfigurationProperties 默认不生效, 需要 SEC.6 的
 * @EnableConfigurationProperties(JwtProperties.class) 才会注册成 Bean.
 */
@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** 签名密钥 (必填, 不给默认值, 强制 yml 配) */
    private String secret;

    /** Token 过期时长 ms (默认 2 小时) */
    private Long expiration = 7200000L;
}