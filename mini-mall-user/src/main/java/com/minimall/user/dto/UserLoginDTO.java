package com.minimall.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录入参 DTO
 *
 * 加 @NotBlank 校验：
 *   - 空串 / null / 全空格 都会被拒
 *   - 失败时会抛 MethodArgumentNotValidException
 *     → GlobalExceptionHandler 接住后返 400 + 字段名+原因
 */
@Data
public class UserLoginDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
