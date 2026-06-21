package com.minimall.review.client.fallback;

import com.minimall.common.core.domain.Result;
import com.minimall.review.client.ProductFeignClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * ProductFeignClient 降级实现 (review 服务侧)
 * <p>
 * 触发场景同 OrdersFeignClientFallback.
 * <p>
 * 设计原则: refreshRating 是【非关键路径】(不影响评价落库, 只影响商品页评分实时性)
 *   降级时: 评价【已经存进 reviews 表】, 只是 product.avg_rating 这次没更新
 *   下次再有人评价同商品时会再次触发 refresh, 旧值会被覆盖
 *   ⇒ 业务层【容忍这次失败】, fallback 只 log 不抛错
 *
 * 跟 order 的 ProductFeignClientFallback.restoreStock 同思路:
 *   "不能因为补偿失败就把主流程炸掉, log 一下让人工/job 后续补救"
 */
@Component
public class ProductFeignClientFallback implements ProductFeignClient {

    private static final Logger log = LoggerFactory.getLogger(ProductFeignClientFallback.class);

    /**
     * ⭐ TODO ② 用户写: refreshRating 降级方法体
     *
     * 提示 (跟 order 服务里 ProductFeignClientFallback.restoreStock 套路一样):
     *   1. log.warn 打个降级日志 (含 productId)
     *      警示语: "[Feign-Fallback] refreshRating 降级(评分未刷新) productId={}"
     *   2. ⚠ 这里【不返 error】 — 评价主流程不该因为评分没刷新就失败
     *      return Result.success(null);
     */
    @Override
    public Result<Void> refreshRating(Long productId) {
        log.warn("[Feign-Fallback] refreshRating 降级(评分未刷新) productId={}", productId);
        return Result.success(null);     // ✅ 让主流程继续
    }

}
