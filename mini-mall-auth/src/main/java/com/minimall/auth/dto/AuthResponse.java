package com.minimall.auth.dto;

import com.minimall.auth.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 认证响应 (统一 login/register/oauth 三个端点的返回)
 *
 * 在 user 服务时:
 *   /user/login   返 String token
 *   /user/oauth/* 返 Map { token, user }
 * 抽到 auth 后统一成这个类:
 *   token = mini-mall 自家 JWT, 前端 localStorage 存
 *   user  = 用户基本信息, 前端展示用 (password 字段已 @JsonIgnore)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private User user;
}
