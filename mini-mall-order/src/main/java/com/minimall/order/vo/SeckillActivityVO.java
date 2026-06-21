package com.minimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀活动 VO (前端列表用, 从单体搬 0 改动)
 *
 * 字段组合: 活动自己的字段 + 商品快照字段 (productName/Image/originalPrice)
 * 商品字段在 service 层调 Feign 拿到后填入
 */
@Data
public class SeckillActivityVO {
    private Long id;
    private Long productId;
    private String productName;     // 从 product 服务查
    private String productImage;    // 从 product 服务查
    private BigDecimal originalPrice;// 商品原价 (供前端展示"立省 X 元")
    private BigDecimal seckillPrice;
    private Integer stock;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Byte status;
    private String statusDesc;       // 状态中文 "待开始/进行中/已结束"
}
