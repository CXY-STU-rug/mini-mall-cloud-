package com.minimall.product.client.fallback;

import com.minimall.common.core.domain.Result;
import com.minimall.product.client.ReviewFeignClient;
import com.minimall.product.vo.ReviewStatsVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * ReviewFeignClient 降级实现
 * <p>
 * 触发场景: review 服务挂了 / Sentinel 熔断.
 * <p>
 * 设计原则: refreshRating 是【非关键路径】(评价主流程已经成功落库, 这里只是异步刷分):
 *   返默认 "0 分 0 条", 让上游照常 UPDATE product.avg_rating=0
 *   ⚠ 这会把已有的评分清 0! 不太理想
 *   ✓ 更好的策略: fallback 返 null + 上游判 null 时跳过 UPDATE
 *   → 教学版选第二种, 更安全
 */
@Component
public class ReviewFeignClientFallback implements ReviewFeignClient {

    private static final Logger log = LoggerFactory.getLogger(ReviewFeignClientFallback.class);

    @Override
    public Result<ReviewStatsVO> getStats(Long productId) {
        log.warn("[Feign-Fallback] review.getStats 降级(评分本次不刷新) productId={}", productId);
        // 返 success + data=null, 上游 refreshRating 判 null 时跳过 UPDATE
        return Result.success(null);
    }
}
