package com.minimall.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册入参 DTO
 *
 * 字段策略:
 *   username  必填  C 端登录用
 *   password  必填  BCrypt 加密入库
 *   phone     选填  C 端 WEB.2 注册表单收集; 手机号格式校验
 *   nickname  选填  没填则前端默认显示 username
 */
@Data
public class UserRegisterDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度 3~20")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度 6~50")
    private String password;

    /**
     * 手机号 (选填)
     * @Pattern 是 Bean Validation 的正则校验, 入参不符合会被 @Valid 拦下返 400.
     * 正则: 中国大陆 11 位手机号, 1 开头, 第 2 位 3-9.
     * 注: null 会跳过 @Pattern 校验, 但空串 "" 会失败 — 前端不传字段就行, 不要传空串.
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不对")
    private String phone;

    /**
     * 昵称 (选填)
     * 不传时 user 表 nickname 字段为 null, 前端展示用 username 兜底.
     */
    @Size(max = 20, message = "昵称不超过 20 字")
    private String nickname;
}
