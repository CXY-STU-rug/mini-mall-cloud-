package com.minimall.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.minimall.user.entity.Coupon;
import com.minimall.user.vo.UserCouponVO;

import java.math.BigDecimal;
import java.util.List;

/**
 * 优惠券 Service (G8)
 * <p>
 * 5 个对外方法 + 2 个 internal (给 order 服务 Feign 调).
 */
public interface ICouponService extends IService<Coupon> {

    // ════════════════════════════════════════════════════════════
    // 用户/管理 API
    // ════════════════════════════════════════════════════════════

    /** 创建券模板 (G8 教学用, 实际项目走管理后台) */
    Long createCoupon(Coupon coupon);

    /** 列出当前可领的券: status=1 且在有效期 (含 remain_stock>0) */
    List<Coupon> listAvailable();

    /**
     * 用户领券 (核心)
     * 业务规则:
     *   1. 券存在 + status=1 + 在有效期
     *   2. remain_stock > 0  (CAS 原子扣减)
     *   3. 该用户没领过这张券 (UNIQUE KEY 兜底)
     *   4. INSERT user_coupon
     */
    void receive(Long userId, Long couponId);

    /** "我的券"列表 (Service 算 expired 标志, 前端直接用) */
    List<UserCouponVO> listMine(Long userId);

    // ════════════════════════════════════════════════════════════
    // Internal API (order 服务 Feign 调)
    // ════════════════════════════════════════════════════════════

    /**
     * 用券 (下单时调)
     * 校验:
     *   1. user_coupon 存在 + status=0 (未用)
     *   2. user_coupon.user_id == 传入的 userId (防越权)
     *   3. coupon 在有效期
     *   4. orderAmount >= coupon.threshold (满减门槛)
     * 成功:
     *   - UPDATE user_coupon SET status=1, use_time=NOW, order_id=?
     *   - 返回【实际抵扣金额】
     */
    BigDecimal useCoupon(Long userId, Long userCouponId, BigDecimal orderAmount, Long orderId);

    /**
     * 退券 (取消订单/关单时调)
     * UPDATE user_coupon SET status=0, use_time=NULL, order_id=NULL WHERE id=?
     */
    void refundCoupon(Long userCouponId);
}
