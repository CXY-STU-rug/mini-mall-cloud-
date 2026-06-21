package com.minimall.product.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 评分聚合 VO (product 侧, 跟 review 服务那边字段对齐)
 * <p>
 * 没抽 mini-mall-review-api 公共模块, 两边各维护一份.
 * 改字段时必须同步两边, 否则 Jackson 反序列化字段对不上.
 */
@Data
public class ReviewStatsVO {
    private BigDecimal avgRating;
    private Integer reviewCount;
}
