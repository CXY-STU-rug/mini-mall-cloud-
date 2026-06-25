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
        // 兼容老调用 (没 role): 默认 role=0 (普通用户)
        return generateToken(userId, username, (byte) 0);
    }

    /**
     * ADMIN 阶段新增重载: 把 role 也塞 JWT
     * 网关 AuthGlobalFilter 解 token 后可以直接读 role 判断是否管理员, 不用每次查 DB.
     */
    public String generateToken(Long userId, String username, Byte role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);

        return Jwts.builder()
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getExpiration()))
                .signWith(getSigningKey())
                .compact();
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

    /** 从 token 提取 role; 老 token 没塞 role 时返回 null, 调用方按非管理员处理 */
    public Byte getRoleFromToken(String token) {
        Integer role = parseToken(token).get("role", Integer.class);   // JWT 反序列化 Byte 会变 Integer
        return role == null ? null : role.byteValue();
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