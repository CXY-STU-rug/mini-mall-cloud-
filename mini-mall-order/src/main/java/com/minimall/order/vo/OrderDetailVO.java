package com.minimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单详情 VO (从单体搬, 0 改动)
 *
 * 跟 OrderListVO 差异:
 *   多了 phone / remark / payTime / shipTime / finishTime (列表页不展示这些细节)
 */
@Data
public class OrderDetailVO {
    private Long orderId;
    private String orderNo;
    private Byte status;
    private String statusDesc;
    private BigDecimal totalAmount;
    private String receiver;
    private String phone;            // ← 详情多
    private String address;
    private String remark;           // ← 详情多
    private LocalDateTime payTime;   // ← 详情多
    private LocalDateTime shipTime;  // ← 详情多
    private LocalDateTime finishTime;// ← 详情多
    private LocalDateTime createTime;
    // ─── G6 物流字段 (status>=2 时有值, 之前为 null) ────
    private String logisticsNo;
    private String logisticsCompany;
    // ───────────────────────────────────────────────────
    private List<OrderItemVO> items;
}
