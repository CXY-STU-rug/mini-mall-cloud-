package com.minimall.review.client.fallback;

import com.minimall.common.core.domain.Result;
import com.minimall.review.client.OrdersFeignClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * OrdersFeignClient 降级实现
 * <p>
 * 触发场景:
 *   - order 服务挂了 (Connection refused)
 *   - Sentinel 熔断打开 (异常率达阈值)
 *   - 调用方主动 hardcode 走 fallback
 * <p>
 * 设计原则: 评价是【弱业务】 (用户体验受影响, 但不丢钱):
 *   getOrderDetail 降级 → 直接返 503, 上层 Service 拿到 code != 200
 *   就抛 BusinessException("订单服务不可用, 请稍后再评价"),
 *   前端看到友好提示, 而不是裸 500.
 */
@Component
public class OrdersFeignClientFallback implements OrdersFeignClient {

    private static final Logger log = LoggerFactory.getLogger(OrdersFeignClientFallback.class);

    /**
     * ⭐ TODO ① 用户写: getOrderDetail 降级方法体
     *
     * 提示 (跟 order 服务里 ProductFeignClientFallback.getById 套路一样):
     *   1. log.warn 打个降级日志 (含 orderId/userId 方便排查)
     *   2. return Result.error(503, "订单服务暂不可用");
     *
     * 参数说明:
     *   - orderId: 用户要评价的订单
     *   - userId:  X-User-Id 透传过来的当前评价人
     */
    @Override
    public Result<Map<String, Object>> getOrderDetail(Long orderId, Long userId) {
        // 前置校验失败 → 必须拦下, 评价不能继续
        log.warn("[Feign-Fallback] order.getOrderDetail 降级 orderId={} userId={}", orderId, userId);
        return Result.error(503, "订单服务暂不可用, 请稍后再评价");
    }
}
