package com.minimall.user.vo;

import lombok.Data;

@Data
public class UserProfileVO {
    private Long id;
    private String username;
    private String nickname;
    private String phone;
    private String email;
    private String avatar;

}
