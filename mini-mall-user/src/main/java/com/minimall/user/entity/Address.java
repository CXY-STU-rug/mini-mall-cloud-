package com.minimall.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 收货地址表 (从单体 com.minimall.minimall.entity 搬过来)
 *
 * 迁移变化:
 *   - 包名: com.minimall.minimall.entity → com.minimall.user.entity
 *   - 其他 0 改动
 *
 * 表结构 (跟单体共用同一张 mini_mall.address 表):
 *   id / user_id / receiver / phone / province / city / district / detail / is_default
 *   create_time / update_time / is_deleted
 */
@Getter
@Setter
public class Address implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户 ID (关键: 微服务里这个值从网关 X-User-Id header 来) */
    private Long userId;

    /** 收货人 */
    private String receiver;

    /** 手机号 */
    private String phone;

    private String province;
    private String city;
    private String district;

    /** 详细地址 */
    private String detail;

    /** 是否默认: 0 否 1 是 */
    private Byte isDefault;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Byte isDeleted;
}
