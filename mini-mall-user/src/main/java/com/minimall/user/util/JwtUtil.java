package com.minimall.user.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类
 *
 * 跟单体里的 JwtUtil 几乎一模一样，只改了包名：
 *   com.minimall.minimall.common.util  →  com.minimall.user.util
 *
 * ⚠️ 注意 javax.crypto.SecretKey 不需要改成 jakarta.crypto
 *   这是 JCE（Java Cryptography Extension）标准包，跟 Servlet/EE 改名无关
 *   Boot 3 只改了 javax.servlet/javax.persistence 这些，javax.crypto 保留
 *
 * 现在它只在 user-service 里用（登录签发 token）。
 * 将来 Gateway 也要解 Token 时，会把这个类抽到 mini-mall-common-security 模块（届时再说）。
 */
/**
 * F1 新增 @RefreshScope:
 *   配合 Nacos Config 实现动态刷新
 *   - 当 Nacos 上的 jwt.expiration / jwt.secret 改变时
 *   - Spring 销毁本 Bean 实例, 重新创建并重新执行 @Value 注入
 *   - 字段拿到新值, 不用重启服务
 *
 * 没标 @RefreshScope 时:
 *   @Value 只在启动时执行一次, 字段值固化, 远程改了字段不变
 */
@Component
@RefreshScope
public class JwtUtil {

    // 从 yml 读密钥（jwt.secret）
    @Value("${jwt.secret}")
    private String secret;

    // 从 yml 读过期时间毫秒（jwt.expiration）
    @Value("${jwt.expiration}")
    private Long expiration;

    /**
     * 生成 token
     *
     * 流程：业务数据 → claims Map → Jwts.builder 签名 → 字符串 token
     */
    public String generateToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);

        return Jwts.builder()
                .claims(claims)
                .issuedAt(new Date())                                            // 签发时间
                .expiration(new Date(System.currentTimeMillis() + expiration))   // 过期时间
                .signWith(getSigningKey())                                       // 签名
                .compact();                                                       // 输出 xxx.yyy.zzz 字符串
    }

    /**
     * 解析 token，验签 + 返回 Claims
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())     // 验证签名（密钥对不上会抛异常）
                .build()
                .parseSignedClaims(token)
                .getPayload();                    // 拿 claims 数据
    }

    /** 从 token 提取 userId */
    public Long getUserIdFromToken(String token) {
        return parseToken(token).get("userId", Long.class);
    }

    /** 从 token 提取 username */
    public String getUsernameFromToken(String token) {
        return parseToken(token).get("username", String.class);
    }

    /**
     * 生成 SecretKey（HMAC 算法对称密钥）
     * 库的固定写法
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
