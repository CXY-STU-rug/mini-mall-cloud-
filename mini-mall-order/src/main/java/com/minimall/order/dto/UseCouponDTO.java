package com.minimall.order.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 用券入参 DTO (order 调 user 的 Feign 用)
 * <p>
 * 跟 user 服务那边的 UseCouponDTO 字段对应 (字段名要一致, Jackson 反序列化按 name).
 * 教学没抽 mini-mall-user-api 公共模块, 两边各维护一份, 改字段时要同步两边.
 */
@Data
public class UseCouponDTO {
    private Long userId;
    private Long userCouponId;
    private BigDecimal orderAmount;
    private Long orderId;
}
