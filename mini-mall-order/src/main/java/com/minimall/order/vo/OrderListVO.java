package com.minimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单列表 VO ("我的订单"页面用, 从单体搬, 0 改动)
 *
 * 一个订单 + 它的所有明细组合返给前端, 减少前端再请求一次的开销.
 */
@Data
public class OrderListVO {
    private Long orderId;
    private String orderNo;
    private Byte status;             // 原始状态 0~4
    private String statusDesc;       // 中文 "待付款" 等
    private BigDecimal totalAmount;
    private String receiver;
    private String address;
    private LocalDateTime createTime;
    private List<OrderItemVO> items; // ⭐ 一对多, 这单的所有明细
}
