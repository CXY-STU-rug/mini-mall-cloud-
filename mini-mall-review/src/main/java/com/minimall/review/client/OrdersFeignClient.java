package com.minimall.review.client;

import com.minimall.common.core.domain.Result;
import com.minimall.review.client.fallback.OrdersFeignClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * Order 服务 Feign 客户端 (review 服务侧)
 * <p>
 * 用途: 评价前【校验订单】
 *   评价业务规则:
 *     ① 订单存在
 *     ② 订单所有者 == 当前评价人  (用 X-User-Id 透传, order 服务那边自带校验)
 *     ③ 订单状态 = COMPLETED / SIGNED  (review 拿到 Map 里读 status 判)
 *     ④ 订单里【真包含】这个 productId   (review 在 Service 层判 items 里有没有)
 * <p>
 * 跟 H1 阶段写的 ProductFeignClient/UserFeignClient 套路完全一样:
 *   1. @FeignClient(name="mini-mall-order")  → 走 Nacos + LoadBalancer
 *   2. fallback=OrdersFeignClientFallback    → order 挂了走降级
 *   3. 方法签名跟 order 的 Controller 完全对齐 (path/method/header)
 * <p>
 * ⭐ SEC.11 重构后: 不再需要 @RequestHeader 形参.
 *     common-security 的 FeignAuthInterceptor 在每次 Feign 出站时
 *     自动从 SecurityContextHolder 取 userId 塞 X-User-Id 头.
 *     下游 order 的 HeaderInterceptor 同样会从头部读出来放进自己的 ThreadLocal.
 */
@FeignClient(
        name = "mini-mall-order",
        fallback = OrdersFeignClientFallback.class
)
public interface OrdersFeignClient {

    /**
     * 查订单详情 (调 order 服务的 GET /order/{orderId})
     * <p>
     * 返回 Map 里至少有: status / items (商品列表) / userId
     * 注意 Map<String, Object> 是【简化兼容方案】, 同 H1 风格,
     *   生产环境应抽 mini-mall-order-api 模块共享 VO.
     */
    @GetMapping("/order/{orderId}")
    Result<Map<String, Object>> getOrderDetail(
            @PathVariable("orderId") Long orderId
    );
}
