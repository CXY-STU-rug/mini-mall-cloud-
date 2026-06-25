package com.minimall.auth.model;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * auth 服务的 User POJO (精简版, 不带 MyBatis-Plus 注解)
 *
 * 跟 user 服务 entity.User 字段一一对应, 但:
 *   - 没有 @TableId / @TableLogic 等 MP 注解 (auth 不操作 DB)
 *   - 包名是 com.minimall.auth.model (跟 user.entity 区分开)
 *
 * 为啥不抽 user-api 模块共享?
 *   - 抽模块开销大 (新 pom + 改两个服务依赖)
 *   - 字段就 12 个, 短期内变化不会快
 *   - 等之后 product/order 也要操作 user 时再抽
 *
 * 反序列化: Feign 把 user 服务返回的 JSON 映射到本类时, 按【字段名匹配】
 *           只要字段名一致 (id/username/...) 就能填上, 跟 entity.User 是不是同一个类无关
 *           这就是 Jackson 序列化的优势: 跨服务边界后只剩 JSON, 没有 Java 类型耦合
 */
@Data
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;

    /**
     * BCrypt 密文.
     * ⚠️ 这里【没有 @JsonIgnore】, 因为 Feign 跨服务要序列化 password (注册/登录都需要密文).
     * 返给前端前必须 Controller 手动 setPassword(null), 兜底见 AuthController.toSafe(user).
     */
    private String password;

    private String nickname;
    private String phone;
    private String email;
    private String avatar;
    private Byte role;
    private Byte status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Byte isDeleted;
    private String oauthProvider;
    private String oauthId;
}
