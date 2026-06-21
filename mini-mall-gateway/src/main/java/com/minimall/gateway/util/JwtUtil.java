package com.minimall.gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * JWT 工具类（gateway 版）
 *
 * ⚠️ 这是从 mini-mall-user 模块直接复制过来的简化版
 *
 * 跟 user 版的区别：
 *   ① 包名不同：com.minimall.gateway.util（vs com.minimall.user.util）
 *   ② 删掉了 generateToken 方法 —— 网关不签发 token，只解析
 *      （签发还是 user-service 在登录时干，网关接到的 token 都是登录过的）
 *
 * 跟单体版的区别：
 *   ① 也只保留 parseToken 部分
 *   ② @Value 从 yml 读 jwt.secret —— gateway 的 yml 要配
 *
 * 将来理想的归处：抽 mini-mall-common-security 模块，user 和 gateway 共用一份
 *   现阶段先复制让 D3 流程跑通，重构是 D 阶段完成后的事
 */
@Component
public class JwtUtil {

    // 从 gateway/application.yml 读密钥
    // ⚠️ 必须跟 user 服务的 jwt.secret 完全一致！否则解 token 必失败
    @Value("${jwt.secret}")
    private String secret;

    /**
     * 解析 token，返回 Claims（含 userId / username 等）
     *
     * 内部做了 3 件事：
     *   ① verifyWith(key) —— 用密钥验证签名（不对抛 SignatureException）
     *   ② 检查 expiration —— 过期抛 ExpiredJwtException
     *   ③ 返回 payload —— 业务数据
     *
     * 调用方拿到 Claims 后，用 .get("userId", Long.class) 取字段
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** 从 token 提取 userId（最常用） */
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
