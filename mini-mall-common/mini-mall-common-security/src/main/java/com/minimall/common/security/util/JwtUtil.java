package com.minimall.common.security.util;

import com.minimall.common.security.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类 - 公共版 (合并自 user/util/JwtUtil + gateway/util/JwtUtil)
 *
 * 职责:
 *   ① generateToken  ← user 服务登录时签发 token
 *   ② parseToken     ← gateway 鉴权时解析, 任何业务服务可调
 *   ③ getXxxFromToken ← parseToken 的便捷快捷方式
 *
 * 用法 (业务代码):
 *   @Autowired private JwtUtil jwtUtil;
 *   String token = jwtUtil.generateToken(123L, "alice");
 *   Long uid = jwtUtil.getUserIdFromToken(token);
 *
 * 为什么用 @RefreshScope:
 *   配合 Nacos Config 热刷新. Nacos 上改了 jwt.secret 后,
 *   Spring 销毁本 Bean 重新创建, JwtProperties 重新注入新值, 不用重启.
 *
 * ⚠️ javax.crypto.SecretKey 不需要改成 jakarta, JCE 标准包跟 Servlet/EE 改名无关.
 */
@Component
@RefreshScope
public class JwtUtil {

    /**
     * TODO ① 字段注入 JwtProperties
     * 提示: @Autowired private JwtProperties jwtProperties;
     */
    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 签发 token (user 登录用)
     * 流程: 业务数据 → claims Map → Jwts.builder 签名 → xxx.yyy.zzz 字符串
     *
     * TODO ②: 把方法体补全, 参考下面老 user/util/JwtUtil 的实现, 只把
     *   - secret           → jwtProperties.getSecret()
     *   - expiration       → jwtProperties.getExpiration()
     * 其他保持原样
     */
    public String generateToken(Long userId, String username) {
        // 1. 业务数据塞 claims (token 的 payload 段)
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);

        // 2. Jwts.builder() 链式生成: claims + 时间戳 + 签名 → xxx.yyy.zzz
        //    注意: 0.12.x 用 .claims() / .issuedAt() / .expiration() (无 set 前缀)
        return Jwts.builder()
                .claims(claims)                                                          // payload 业务数据
                .issuedAt(new Date())                                                    // iat 签发时间
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getExpiration())) // exp 过期时间
                .signWith(getSigningKey())                                               // 用密钥签名
                .compact();                                                              // 输出最终字符串
    }

    /**
     * 解析 token, 验签 + 返回 Claims (gateway / 业务服务都可能调)
     *
     * TODO ③: 补全方法体
     * 提示: 跟老 user 版的 parseToken 一模一样, 替换 secret 来源即可
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
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
     * 生成 SecretKey (HMAC 对称密钥)
     *
     * TODO ④: 用 jwtProperties.getSecret() 生成
     * 提示: Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}