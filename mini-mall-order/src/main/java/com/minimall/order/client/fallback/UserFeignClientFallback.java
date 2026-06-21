package com.minimall.order.client.fallback;

import com.minimall.common.core.domain.Result;
import com.minimall.order.client.UserFeignClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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
}
