package com.minimall.review.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品评分聚合 VO (G7 重构: 给 product 服务 Feign 调)
 * <p>
 * 比 Map<String, Object> 干净, 类型安全, 前端/Feign 反序列化稳.
 */
@Data
public class ReviewStatsVO {
    /** 平均评分 0.0~5.0 (无评价时 null) */
    private BigDecimal avgRating;
    /** 评价总数 (无评价时 0) */
    private Integer reviewCount;
}
