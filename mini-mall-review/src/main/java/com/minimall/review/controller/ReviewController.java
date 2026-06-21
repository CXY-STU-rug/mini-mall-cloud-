package com.minimall.review.controller;

import com.minimall.common.core.domain.Result;
import com.minimall.review.dto.CreateReviewDTO;
import com.minimall.review.service.IReviewsService;
import com.minimall.review.vo.ReviewStatsVO;
import com.minimall.review.vo.ReviewVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 评价 Controller (G7 - 3 个端点)
 * <p>
 * 网关代理 /review/** → mini-mall-review (9004):
 *   POST /review                     → 创建评价  (需登录)
 *   GET  /review/product/{productId} → 商品评价列表 (公开, 网关白名单已加)
 *   GET  /review/user                → 我的评价   (需登录)
 * <p>
 * 路径设计原则:
 *   - /review                       动作集中, 写评价
 *   - /review/product/...           资源是商品的评价
 *   - /review/user                  资源是我自己的评价
 * <p>
 * userId 来源: 网关解 JWT 后透传的 X-User-Id header (B 阶段 + D3 已建立)
 *   不从前端 body 拿 - 前端可篡改
 */
@RestController
@RequestMapping("/review")
public class ReviewController {

    @Autowired
    private IReviewsService reviewsService;

    // ════════════════════════════════════════════════════════════════
    // ① 创建评价
    // ════════════════════════════════════════════════════════════════
    /**
     * 入参 @Valid 触发 CreateReviewDTO 上的 @NotNull/@Min/@Max/@Size 校验
     *   - 失败时 MethodArgumentNotValidException 由 GlobalExceptionHandler 翻译成 400
     *
     * 业务异常 (订单不存在/状态不对/不在订单里/重复评价) 由 Service 抛 BusinessException
     *   GlobalExceptionHandler 兜底翻译成对应错误码
     */
    @PostMapping
    public Result<Long> create(
            @Valid @RequestBody CreateReviewDTO dto,
            @RequestHeader("X-User-Id") Long userId
    ) {
        Long reviewId = reviewsService.createReview(userId, dto);
        return Result.success(reviewId);
    }

    // ════════════════════════════════════════════════════════════════
    // ② 查指定商品的评价列表 (公开 - 网关白名单 "/review/product")
    // ════════════════════════════════════════════════════════════════
    @GetMapping("/product/{productId}")
    public Result<List<ReviewVO>> listByProduct(@PathVariable Long productId) {
        return Result.success(reviewsService.listByProduct(productId));
    }

    // ════════════════════════════════════════════════════════════════
    // ③ 查"我的"评价 (需登录)
    // ════════════════════════════════════════════════════════════════
    @GetMapping("/user")
    public Result<List<ReviewVO>> listMyReviews(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(reviewsService.listMyReviews(userId));
    }

    // ════════════════════════════════════════════════════════════════
    // ④ Internal: 评分聚合 (给 product 服务 Feign 调, G7 重构)
    //    路径 /internal 是约定, 走 Feign 直连 review:9004, 不该被前端访问
    // ════════════════════════════════════════════════════════════════
    @GetMapping("/internal/stats/{productId}")
    public Result<ReviewStatsVO> getStats(@PathVariable Long productId) {
        Map<String, Object> stats = reviewsService.getStats(productId);
        ReviewStatsVO vo = new ReviewStatsVO();
        // Map 里数据类型不固定, 一律 toString 后构造, 防 Long/Integer/BigDecimal 反序列化爆雷
        Object avg = stats.get("avgRating");
        Object cnt = stats.get("reviewCount");
        vo.setAvgRating(avg == null ? null : new BigDecimal(avg.toString()));
        vo.setReviewCount(cnt == null ? 0 : ((Number) cnt).intValue());
        return Result.success(vo);
    }
}
