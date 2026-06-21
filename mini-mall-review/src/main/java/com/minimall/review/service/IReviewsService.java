package com.minimall.review.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.minimall.review.dto.CreateReviewDTO;
import com.minimall.review.entity.Reviews;
import com.minimall.review.vo.ReviewVO;

import java.util.List;

/**
 * 评价服务接口
 * <p>
 * 继承 MyBatis-Plus 的 IService<Reviews>, 自动拿到 list/count/save/getById 等 CRUD 方法.
 * 我们只声明【业务方法】.
 */
public interface IReviewsService extends IService<Reviews> {

    /**
     * 创建评价 (G7 核心)
     * <p>
     * 业务规则 (在 impl 里写校验链):
     *   1. 订单存在 (Feign 调 order)
     *   2. 订单状态 = COMPLETED (=3) 才能评价
     *   3. 该商品确实在订单 items 里
     *   4. 同一 (orderId, productId) 不能重复评价
     *   5. INSERT reviews
     *   6. Feign 调 product.refreshRating(productId) 回写评分
     *
     * @param userId 当前评价人 (从 X-User-Id header 透传)
     * @param dto    前端入参 (orderId/productId/rating/content)
     * @return 新评价的 id
     */
    Long createReview(Long userId, CreateReviewDTO dto);

    /** 查指定商品的所有评价 (商品详情页用, 公开) */
    List<ReviewVO> listByProduct(Long productId);

    /** 查"我的"评价 (我的中心 - 我的评价, 需登录) */
    List<ReviewVO> listMyReviews(Long userId);

    /**
     * 算商品评分聚合 (给 product 服务 Feign 调)
     * <p>
     * 返回 Map: {"avgRating": BigDecimal, "reviewCount": Long}
     * 无评价时 avgRating=null, reviewCount=0
     */
    java.util.Map<String, Object> getStats(Long productId);
}
