package com.minimall.review.client;

import com.minimall.common.core.domain.Result;
import com.minimall.review.client.fallback.ProductFeignClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

/**
 * Product 服务 Feign 客户端 (review 服务侧, 第 3 次)
 * <p>
 * ⭐ N=3 自然到达 ⭐
 *   第 1 次: mini-mall-user/client/ProductFeignClient    (B 阶段)
 *   第 2 次: mini-mall-order/client/ProductFeignClient   (G3.4 + H1)
 *   第 3 次: mini-mall-review/client/ProductFeignClient  (本次 G7.4)
 *
 *   每次都是【独立写】, 字段名/包名稍有差异
 *   → 这次还按【不抽公共模块】的策略, 因为本服务只调一个端点 (refresh-rating)
 *      跟前两次几乎没重复 (前两次主要是 getById / deductStock)
 *   → 真正抽的是 Redis (G7.8), 不是 ProductFeignClient
 * <p>
 * 用途: review 落库后【回写商品评分聚合】
 *   场景: 用户给商品打 5 星 → review 表插入一行 → 触发 product.avg_rating 重算
 * <p>
 * 设计选择: "回写而不是返回值"
 *   方案 A: review 内存里维护评分 → 商品列表不显示评分 ❌
 *   方案 B: product 详情查时实时 AVG(rating) FROM reviews → 慢 ❌
 *   ✓ 方案 C: review 落库后 PUT 通知 product 重算 → 空间换时间 (avg_rating 列存)
 */
@FeignClient(
        name = "mini-mall-product",
        fallback = ProductFeignClientFallback.class
)
public interface ProductFeignClient {

    /**
     * 通知 product 服务重算指定商品的 avg_rating + review_count
     * <p>
     * 端点路径: PUT /product/{id}/internal/refresh-rating  (G7.7 会建)
     *   - internal 前缀 = "服务间内部接口", gateway 不放行, 前端调不到
     *   - 这边 Feign 接口先定义好,
     *     server 端 G7.7 才会建 Controller 实现, 节奏跟着你走
     */
    @PutMapping("/product/{id}/internal/refresh-rating")
    Result<Void> refreshRating(@PathVariable("id") Long productId);
}
