package com.minimall.product.client;

import com.minimall.common.core.domain.Result;
import com.minimall.product.client.fallback.ReviewFeignClientFallback;
import com.minimall.product.vo.ReviewStatsVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Review 服务 Feign 客户端 (product 侧, G7 重构新增)
 * <p>
 * 用途: refreshRating 时调 review 拿评分聚合.
 * <p>
 * ⭐ 教学迭代轨迹:
 *   G7.7 原版: product 直接建了 mapper/ReviewsMapper 查 reviews 表 (共库取巧)
 *   G7 重构:  product 自己不查 reviews 了, 通过 Feign 调 review 服务
 *             → 这才符合"微服务一服务一表"原则
 * <p>
 * ⭐ 循环依赖说明:
 *   review 服务 Feign 调 product.refreshRating  (评价落库后)
 *   product 服务 Feign 调 review.getStats       (这条)
 *   ↑ 看起来循环, 但实际是【调用图】循环不是【依赖图】循环:
 *     - 两个服务【不互相 import 对方的 jar】 (各自有自己的 ReviewStatsVO 副本)
 *     - 启动时 Feign 接口只是网络调用约定, 不强依赖对方在线
 *     - 运行时也没死锁: review→product 是写流程, product→review 是读流程, 时序不交叉
 */
@FeignClient(
        name = "mini-mall-review",
        fallback = ReviewFeignClientFallback.class
)
public interface ReviewFeignClient {

    @GetMapping("/review/internal/stats/{productId}")
    Result<ReviewStatsVO> getStats(@PathVariable("productId") Long productId);
}
