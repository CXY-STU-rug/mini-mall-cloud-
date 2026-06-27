package com.minimall.user.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户资料更新入参 DTO (PUT /user/me)
 *
 * 字段策略:
 *   - 4 个全部选填, null 表示这次不改这个字段
 *   - MP updateById + 默认空字段不更新策略, 一起协作实现"局部更新"
 *   - 故意没放 username/role/status/password, 用户自己改不了这些 (字段白名单防越权)
 *
 * @Pattern / @Email / @Size 都是 Bean Validation 规则:
 *   入参不符合 → @Valid 拦下 → GlobalExceptionHandler 返 400 + message
 *   null 会跳过 @Pattern / @Email, 但 "" 会失败 — 前端不传字段就行, 别传空串
 */
@Data
public class UserProfileUpdateDTO {

    @Size(max = 20, message = "昵称不超过 20 字")
    private String nickname;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不对")
    private String phone;

    @Email(message = "邮箱格式不对")
    private String email;

    @Size(max = 255, message = "头像 URL 太长")
    private String avatar;

}
