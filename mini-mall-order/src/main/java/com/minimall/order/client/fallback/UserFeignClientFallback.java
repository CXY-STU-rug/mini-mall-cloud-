package com.minimall.order.client.fallback;

import com.minimall.common.core.domain.Result;
import com.minimall.order.client.UserFeignClient;
import com.minimall.order.dto.UseCouponDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * UserFeignClient 的降级实现 (H1.2)
 *
 * 调 user 服务挂了 → createOrder 的地址校验失败 → 返 503 让用户重试.
 * 不能瞎兜底返个假地址, 因为这是订单的核心数据.
 */
@Component
public class UserFeignClientFallback implements UserFeignClient {

    private static final Logger log = LoggerFactory.getLogger(UserFeignClientFallback.class);

    @Override
    public Result<Map<String, Object>> getAddress(Long id, Long userId) {
        log.warn("[Feign-Fallback] user.getAddress 降级 addressId={} userId={}", id, userId);
        return Result.error(503, "用户服务暂不可用,请稍后再试");
    }

    /** G8: 用券降级 - 前置, 必须拦下 (不能假装用券成功导致订单实付错) */
    @Override
    public Result<BigDecimal> useCoupon(UseCouponDTO dto) {
        log.warn("[Feign-Fallback] user.useCoupon 降级 ucId={} userId={}",
                 dto.getUserCouponId(), dto.getUserId());
        return Result.error(503, "用户服务暂不可用,无法使用优惠券");
    }

    /** G8: 退券降级 - 后置补偿, 不能让取消主流程失败 (券补偿可后台 job 重试) */
    @Override
    public Result<Void> refundCoupon(Long ucId) {
        log.warn("[Feign-Fallback] user.refundCoupon 降级(券未退) ucId={}", ucId);
        return Result.success(null);
    }
}
