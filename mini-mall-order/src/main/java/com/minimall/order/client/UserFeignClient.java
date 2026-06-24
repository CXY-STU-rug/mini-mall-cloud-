package com.minimall.order.client;

import com.minimall.common.core.domain.Result;
import com.minimall.order.dto.UseCouponDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.Map;

/**
 * User 服务的 Feign 客户端 (order 服务用, 给 createOrder 拿收货地址)
 *
 * ════════════════════════════════════════════════════════════════
 * 跟 ProductFeignClient 同样套路, 这是 order 服务的第 2 个 Feign Client.
 * (feedback_concrete_first: N=2, 还不抽公共 - 等 N=3 再抽 mini-mall-user-api)
 *
 * 关键点 ⭐⭐⭐ (SEC.11 重构后):
 *   AddressController.detail() 用 SecurityContextHolder.getUserId() 做越权校验.
 *
 *   order 调 user 时, common-security 的 FeignAuthInterceptor 自动从
 *   SecurityContextHolder 取 userId 塞 X-User-Id 头, 这里不再需要显式声明参数.
 * ════════════════════════════════════════════════════════════════
 */
@FeignClient(
        name = "mini-mall-user",
        fallback = com.minimall.order.client.fallback.UserFeignClientFallback.class
)
public interface UserFeignClient {

    /**
     * 调 user 服务的 GET /user/address/{id}
     *
     * @param id     地址 ID
     * @return Result<Map<String, Object>>
     *         Map 里的字段对应 Address entity:
     *           id, userId, receiver, phone, province, city, district, detail, ...
     */
    @GetMapping("/user/address/{id}")
    Result<Map<String, Object>> getAddress(
            @PathVariable("id") Long id
    );

    /**
     * G8: 用券 (下单时调)
     * 成功返【实际抵扣金额】, 失败 (券不属于该用户/已用/过期/未达门槛) 抛 BusinessException
     */
    @PutMapping("/coupon/internal/use")
    Result<BigDecimal> useCoupon(@RequestBody UseCouponDTO dto);

    /**
     * G8: 退券 (取消订单/关单时调)
     * 设计为幂等, 多次调用结果一致, 找不到券 / 已是未用都返 success.
     */
    @PutMapping("/coupon/internal/refund/{ucId}")
    Result<Void> refundCoupon(@PathVariable("ucId") Long ucId);
}
