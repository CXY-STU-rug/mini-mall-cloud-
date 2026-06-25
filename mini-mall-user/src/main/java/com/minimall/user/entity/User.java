package com.minimall.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体类（user 表的 Java 镜像）
 *
 * 跟单体 mini-mall 里的 User.java 几乎一模一样，只改了包名：
 *   com.minimall.minimall.entity  →  com.minimall.user.entity
 *
 * MyBatis-Plus 注解说明：
 *   @TableId(value="id", type=IdType.AUTO) → 主键 id，数据库自增
 *   @TableLogic                              → 逻辑删除字段（is_deleted=1 表示删了，但物理还在）
 *   @JsonIgnore                              → 序列化成 JSON 时跳过这个字段（密码绝不返前端）
 *
 * implements Serializable 的原因：
 *   微服务里 Feign 远程调用、Redis 缓存都要求 POJO 实现 Serializable
 */
@Getter
@Setter
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String username;

    /** 密码 BCrypt 加密后存的，永远不返前端 */
    @JsonIgnore
    private String password;

    private String nickname;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 头像 url */
    private String avatar;

    private Byte role;

    /** 0=禁用,1=正常 */
    private Byte status;

    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除标记，0=未删除 1=已删除 */
    @TableLogic
    private Byte isDeleted;
    /** OAuth 来源: github / wechat / null=本地账号 */
    private String oauthProvider;

    /** OAuth 平台返回的用户唯一 id (GitHub 是数字, 微信是 openid) */
    private String oauthId;
}
