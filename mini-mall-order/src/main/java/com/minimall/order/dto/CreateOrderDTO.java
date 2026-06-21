package com.minimall.order.dto;

import lombok.Data;

import java.util.List;

/**
 * 创建订单 DTO (从单体搬, 0 改动)
 *
 * 前端 POST /order body:
 *   {
 *     "addressId": 5,
 *     "cartItemIds": [12, 13],
 *     "remark": "尽快发货"
 *   }
 */
@Data
public class CreateOrderDTO {
    /** 选哪个收货地址 (会去 user 服务 Feign 查) */
    private Long addressId;

    /** 要下单的购物车项 ID 列表 (用户勾选了哪几个) */
    private List<Long> cartItemIds;

    /** 备注 (可选) */
    private String remark;
}
