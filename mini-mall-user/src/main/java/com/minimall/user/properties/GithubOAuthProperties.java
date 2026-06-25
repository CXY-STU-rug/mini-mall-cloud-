package com.minimall.user.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * GitHub OAuth2 配置
 *
 * 把 application.yml 里 oauth.github.* 段绑到本类。
 *
 * 命名约定:
 *   yml:  oauth.github.client-id          (kebab-case 连字符)
 *   Java: this.clientId                   (camelCase 驼峰)
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

    /** GitHub 授权页 URL: 用户跳转到这里同意授权 */
    private String authorizeUrl;

    /** GitHub 拿 token 的 URL: 后端用 code 换 token */
    private String tokenUrl;

    /** GitHub 拿用户信息的 URL: 用 token 查用户 */
    private String userInfoUrl;
}