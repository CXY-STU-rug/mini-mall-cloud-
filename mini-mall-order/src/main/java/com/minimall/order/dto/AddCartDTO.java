package com.minimall.order.dto;

import lombok.Data;

/**
 * 加入购物车请求体
 *
 * 用 DTO 的好处:
 *   ① 接口契约清晰 (一眼看出前端要传啥)
 *   ② 字段比 Entity 少 (不暴露 id/createTime 这种内部字段)
 *   ③ 加 @Valid 注解能集中校验
 */
@Data
public class AddCartDTO {

    /** 商品 ID */
    private Long productId;

    /** 数量 */
    private Integer quantity;
}
