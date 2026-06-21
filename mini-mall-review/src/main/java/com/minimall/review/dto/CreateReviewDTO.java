package com.minimall.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建评价 - 入参 DTO
 * <p>
 * 前端 POST /review 时发的 JSON, Controller 用 @Valid + @RequestBody 接收
 * <p>
 * 字段【只放前端真正能填的】:
 *   ✓ orderId, productId, rating, content
 *   ✗ userId    → 不从前端传 (前端可篡改), 从 gateway 透传的 X-User-Id header 拿
 *   ✗ id/createTime/isDeleted → 数据库自己生
 *
 * Bean Validation 在哪生效:
 *   Controller 方法签名加 @Valid 才会校验, 不加注解只是装饰
 */
@Data
public class CreateReviewDTO {

    // ⭐ TODO ① orderId
    //   类型: Long
    //   校验: @NotNull(message = "订单ID不能为空")
    // [你写]

@NotNull(message = "订单ID不能为空")
    private Long orderId;
    // ⭐ TODO ② productId
    //   类型: Long
    //   校验: @NotNull(message = "商品ID不能为空")
    // [你写]
    @NotNull(message = "商品ID不能为空")
    private Long productId;
    // ⭐ TODO ③ rating
    //   类型: Integer
    //   校验:
    //     @NotNull(message = "评分不能为空")
    //     @Min(value = 1, message = "评分至少 1 星")
    //     @Max(value = 5, message = "评分最多 5 星")
    // [你写]
    @NotNull(message = "评分不能为空")
         @Min(value = 1, message = "评分至少 1 星")
        @Max(value = 5, message = "评分最多 5 星")
    private Integer rating;

    // ⭐ TODO ④ content (可选)
    //   类型: String
    //   校验: @Size(max = 500, message = "评价内容不能超过 500 字")
    //   注: 不加 @NotNull, 允许只打分不写文字
    // [你写]
    @Size(max = 500, message = "评价内容不能超过 500 字")
    private String content;
}
