package com.minimall.user.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 用券入参 DTO (给 order 服务 Feign 调)
 * <p>
 * Feign 接口里 4 个参数挤 query string 不优雅, 用 DTO body 干净.
 */
@Data
public class UseCouponDTO {
    private Long userId;
    private Long userCouponId;
    private BigDecimal orderAmount;
    private Long orderId;
}
