package com.minimall.review.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.minimall.common.core.domain.Result;
import com.minimall.common.core.exception.BusinessException;
import com.minimall.review.client.OrdersFeignClient;
import com.minimall.review.client.ProductFeignClient;
import com.minimall.review.dto.CreateReviewDTO;
import com.minimall.review.entity.Reviews;
import com.minimall.review.mapper.ReviewsMapper;
import com.minimall.review.service.IReviewsService;
import com.minimall.review.vo.ReviewVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 评价服务实现 (G7 核心)
 * <p>
 * 业务对外: 3 个方法
 *   - createReview     ★ 校验链 + 落库 + Feign 回写评分 (复杂)
 *   - listByProduct    简单 SELECT
 *   - listMyReviews    简单 SELECT
 * <p>
 * 设计要点 (跟 OrdersServiceImpl 对照看):
 *   ① 继承 ServiceImpl<ReviewsMapper, Reviews> → 拿 MP 的 CRUD 工具方法
 *   ② createReview 标 @Transactional, 落库失败时 INSERT 回滚
 *      ⚠ 但 Feign 调 product.refreshRating 失败【不会】触发回滚:
 *        因为 ProductFeignClientFallback.refreshRating 返 success (G7.4 那次的设计)
 *        所以 Spring 看见返 success, 认为业务正常, 不滚.
 *   ③ 没用 @GlobalTransactional (Seata): 评价不涉及钱, 最终一致即可
 */
@Service
public class ReviewsServiceImpl
        extends ServiceImpl<ReviewsMapper, Reviews>
        implements IReviewsService {

    private static final Logger log = LoggerFactory.getLogger(ReviewsServiceImpl.class);

    /** 订单状态: 已完成 (跟 order 服务 OrderStatus.COMPLETED 对齐) */
    private static final int ORDER_STATUS_COMPLETED = 3;

    @Autowired private ReviewsMapper reviewsMapper;
    @Autowired private OrdersFeignClient ordersFeignClient;
    @Autowired private ProductFeignClient productFeignClient;

    // ════════════════════════════════════════════════════════════════
    // ① 创建评价 (G7 核心 - 4 步校验 + 落库 + Feign 回写)
    // ════════════════════════════════════════════════════════════════
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createReview(Long userId, CreateReviewDTO dto) {

        // ─── 第 1 步: Feign 调 order 拿订单详情 ──────────────────
        // SEC.11: X-User-Id 由 FeignAuthInterceptor 自动透传, 不再当形参传
        Result<Map<String, Object>> orderRet =
                ordersFeignClient.getOrderDetail(dto.getOrderId());

        // ✅ TODO ① 校验 Feign 调用结果
        if (orderRet.getCode() != 200) {
            throw new BusinessException(orderRet.getCode(), orderRet.getMessage());
        }

        // 走到这里, 订单确实存在且属于当前用户
        Map<String, Object> order = orderRet.getData();

        // ─── 第 2 步: 校验订单状态 = COMPLETED ─────────────────
        // Map 里 status 是 Integer (JSON number → Jackson 默认包装成 Integer)
        // 数据库 status 是 byte 0-4, 我们要的是 3 (COMPLETED)
        Integer status = (Integer) order.get("status");

        // ✅ TODO ② 校验 status
        if (status == null || status != ORDER_STATUS_COMPLETED) {
            throw new BusinessException("只有已完成订单可评价");
        }

        // ─── 第 3 步: 校验该商品在订单 items 里 ─────────────────
        // 防 "张三买了商品 A, 但偏要评价商品 B"
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) order.get("items");

        // 用 stream 判断有没有任何一行的 productId 等于 dto.productId
        boolean hasProduct = items != null && items.stream().anyMatch(item -> {
            Number pid = (Number) item.get("productId");
            return pid != null && pid.longValue() == dto.getProductId();
        });

        // ✅ TODO ③ 校验 hasProduct
        if (!hasProduct) {
            throw new BusinessException("该商品不在订单里, 无法评价");
        }

        // ─── 第 4 步: 校验未重复评价 (应用层先拦, 数据库 UNIQUE KEY 是兜底) ────
        // 用 MP 的 LambdaQueryWrapper 查 count
        long cnt = reviewsMapper.selectCount(
                new LambdaQueryWrapper<Reviews>()
                        .eq(Reviews::getOrderId, dto.getOrderId())
                        .eq(Reviews::getProductId, dto.getProductId())
        );

        // ⭐ TODO ④ 校验 cnt
        //   if (cnt > 0)
        //       throw new BusinessException("该商品已评价过, 请勿重复");
        // [你写]
        if (cnt > 0)
            throw new BusinessException("该商品已评价过, 请勿重复");

        // ─── 第 5 步: INSERT reviews (落库) ─────────────────────
        Reviews r = new Reviews();
        r.setUserId(userId);
        r.setOrderId(dto.getOrderId());
        r.setProductId(dto.getProductId());
        r.setRating(dto.getRating());
        r.setContent(dto.getContent());
        // createTime/updateTime/isDeleted 走数据库 DEFAULT, 不用手填
        reviewsMapper.insert(r);
        log.info("[Review] 创建评价成功 userId={} orderId={} productId={} reviewId={}",
                userId, dto.getOrderId(), dto.getProductId(), r.getId());

        // ─── 第 6 步: Feign 调 product.refreshRating 异步刷分 ──────
        // ⚠ 关键修复 (G7.E2E 踩的坑): 必须等 review 事务【提交后】再调 Feign,
        //   否则 product 服务那边查 reviews 表【看不到】这条未提交数据,
        //   会把 avg_rating 错刷成 0.
        //   → 用 TransactionSynchronizationManager 注册 afterCommit 回调
        // 这里不 catch, 因为 fallback 已经返 success (G7.4 设计)
        Long pid = dto.getProductId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                productFeignClient.refreshRating(pid);
            }
        });

        return r.getId();
    }

    // ════════════════════════════════════════════════════════════════
    // ② 查商品评价列表 (商品详情页用)
    // ════════════════════════════════════════════════════════════════
    @Override
    public List<ReviewVO> listByProduct(Long productId) {
        // 按 createTime 倒序查 (最新评价在前)
        List<Reviews> list = reviewsMapper.selectList(
                new LambdaQueryWrapper<Reviews>()
                        .eq(Reviews::getProductId, productId)
                        .orderByDesc(Reviews::getCreateTime)
        );
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    // ════════════════════════════════════════════════════════════════
    // ③ 查"我的"评价 (我的中心)
    // ════════════════════════════════════════════════════════════════
    @Override
    public List<ReviewVO> listMyReviews(Long userId) {
        List<Reviews> list = reviewsMapper.selectList(
                new LambdaQueryWrapper<Reviews>()
                        .eq(Reviews::getUserId, userId)
                        .orderByDesc(Reviews::getCreateTime)
        );
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    // ════════════════════════════════════════════════════════════════
    // ④ 评分聚合 (G7.7 重构: 从 product 服务搬过来)
    //    符合"微服务一服务一表": reviews 表的查询逻辑归 review 服务
    // ════════════════════════════════════════════════════════════════
    @Override
    public Map<String, Object> getStats(Long productId) {
        return reviewsMapper.selectStats(productId);
    }

    // ════════════════════════════════════════════════════════════════
    // 内部工具: Reviews → ReviewVO 转换
    // ════════════════════════════════════════════════════════════════
    private ReviewVO toVO(Reviews r) {
        ReviewVO vo = new ReviewVO();
        // BeanUtils.copyProperties 把同名字段自动拷过去
        //   id/userId/orderId/productId/rating/content/createTime 都同名
        //   userName 在 Reviews 里没有, 留 null (将来 Feign 调 user 服务拿)
        BeanUtils.copyProperties(r, vo);
        return vo;
    }
}
