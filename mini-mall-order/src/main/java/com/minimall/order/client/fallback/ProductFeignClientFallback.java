package com.minimall.order.client.fallback;

import com.minimall.common.core.domain.Result;
import com.minimall.order.client.ProductFeignClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * ProductFeignClient 的降级实现 (H1.1)
 *
 * 触发场景:
 *   - product 服务挂了 (Connection refused / Read timeout)
 *   - Sentinel 熔断打开 (异常率/慢调用比例 触发)
 *   - 调用方主动设的 hardcode 降级
 *
 * 设计原则:
 *   - getById:    返空 Map + 错误码, 上游 Service 看 code 判断走兜底逻辑
 *   - deductStock: 必须返 0 (库存扣不成功) → 上游抛"库存服务不可用"
 *   - restoreStock: 必须返 0 但 log warn → 上游 cancel 流程别因还库存失败炸掉
 *                   (cancel 已经把订单状态改成 CANCELLED, 库存补偿可以延后异步做)
 */
@Component
public class ProductFeignClientFallback implements ProductFeignClient {

    private static final Logger log = LoggerFactory.getLogger(ProductFeignClientFallback.class);

    @Override
    public Result<Map<String, Object>> getById(Long id) {
        log.warn("[Feign-Fallback] product.getById 降级 id={}", id);
        return Result.error(503, "商品服务暂不可用");
    }

    @Override
    public Result<Integer> deductStock(Long id, Integer qty) {
        log.warn("[Feign-Fallback] product.deductStock 降级 id={} qty={}", id, qty);
        return Result.error(503, "库存服务暂不可用,请稍后再试");
    }

    @Override
    public Result<Integer> restoreStock(Long id, Integer qty) {
        // ⚠ 这里只 log, 不返 error
        //   cancel/close 流程已经把订单关了, 库存不还能补偿
        //   生产应该把"待补偿库存"记一张 retry 表, 后台 job 重试
        log.warn("[Feign-Fallback] product.restoreStock 降级(库存未还) id={} qty={}", id, qty);
        return Result.success(0);
    }
}
