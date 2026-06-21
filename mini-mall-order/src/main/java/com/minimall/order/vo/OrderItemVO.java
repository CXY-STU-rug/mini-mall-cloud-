package com.minimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单明细 VO (从单体搬, 0 改动)
 *
 * 注意: 字段全部从 order_item 表的【快照】读, 不再去查 product 服务.
 *      快照存在的意义就是: 历史订单不被商品后续变更影响.
 */
@Data
public class OrderItemVO {
    private Long orderItemId;
    private Long productId;
    private String productName;   // 快照
    private String productImage;  // 快照
    private BigDecimal price;     // 快照
    private Integer quantity;
    private BigDecimal subtotal;
}
