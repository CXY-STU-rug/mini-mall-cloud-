package com.minimall.auth.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * GitHub OAuth2 配置 (从 user 服务搬到 auth 服务, 包名变了, 内容不变)
 *
 * 把 application.yml 里 oauth.github.* 段绑到本类.
 *   yml:  oauth.github.client-id          (kebab-case)
 *   Java: this.clientId                   (camelCase)
 *   Spring Boot 自动映射, 不用我们干啥
 */
@Data
@Component
@ConfigurationProperties(prefix = "oauth.github")
public class GithubOAuthProperties {

    /** GitHub OAuth App 的 Client ID (公开) */
    private String clientId;

    /** GitHub OAuth App 的 Client Secret (私密) */
    private String clientSecret;

    /** OAuth 回调地址, 必须跟 GitHub OAuth App 注册时填的一致 */
    private String callbackUrl;

    /** GitHub 授权页 URL */
    private String authorizeUrl;

    /** GitHub 拿 token 的 URL */
    private String tokenUrl;

    /** GitHub 拿用户信息的 URL */
    private String userInfoUrl;
}
