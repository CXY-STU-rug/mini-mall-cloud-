package com.minimall.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理员发布秒杀活动的入参 (从单体搬, 0 改动)
 *
 * 不含 id / status / createTime, 这些后端自动生成
 */
@Data
public class SeckillActivityDTO {
    private Long productId;
    private BigDecimal seckillPrice;
    private Integer stock;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
