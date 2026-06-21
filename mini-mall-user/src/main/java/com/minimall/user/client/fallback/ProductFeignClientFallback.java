package com.minimall.user.client.fallback;

import com.minimall.common.core.domain.Result;
import com.minimall.user.client.ProductFeignClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * user 服务调 product 的 Feign 降级 (H1.3)
 *
 * user 服务里这个 Feign 主要用在 UserController 的演示接口
 * /user/{userId}/with-product/{productId} 验证 Feign 链路.
 *
 * 降级策略: product 挂了返 503 兜底, 不影响 user 服务本身鉴权/注册等核心接口.
 */
@Component
public class ProductFeignClientFallback implements ProductFeignClient {

    private static final Logger log = LoggerFactory.getLogger(ProductFeignClientFallback.class);

    @Override
    public Result<Map<String, Object>> getById(Long id) {
        log.warn("[Feign-Fallback] (user→product).getById 降级 id={}", id);
        return Result.error(503, "商品服务暂不可用");
    }
}
