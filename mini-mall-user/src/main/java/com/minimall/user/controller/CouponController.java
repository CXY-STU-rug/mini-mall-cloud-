package com.minimall.user.controller;

import com.minimall.common.core.domain.Result;
import com.minimall.user.dto.UseCouponDTO;
import com.minimall.user.entity.Coupon;
import com.minimall.user.service.ICouponService;
import com.minimall.user.vo.UserCouponVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 优惠券 Controller (G8 - 6 端点)
 * <p>
 * 网关代理 /coupon/** → mini-mall-user (9001):
 *   POST /coupon                  创建券模板  (G8 教学暂未加管理员校验)
 *   GET  /coupon/available        当前可领的券 (公开, 白名单)
 *   POST /coupon/{id}/receive     领券        (需登录)
 *   GET  /coupon/mine             我的券       (需登录)
 *   PUT  /coupon/internal/use     Feign: 用券  (order 服务调)
 *   PUT  /coupon/internal/refund/{ucId}  Feign: 退券
 */
@RestController
@RequestMapping("/coupon")
public class CouponController {

    @Autowired
    private ICouponService couponService;

    // ════════════════════════════════════════════════════════════
    // ① 创建券模板 (管理后台)
    // ════════════════════════════════════════════════════════════
    @PostMapping
    public Result<Long> create(@RequestBody Coupon coupon) {
        return Result.success(couponService.createCoupon(coupon));
    }

    // ════════════════════════════════════════════════════════════
    // ② 当前可领的券 (公开)
    // ════════════════════════════════════════════════════════════
    @GetMapping("/available")
    public Result<List<Coupon>> available() {
        return Result.success(couponService.listAvailable());
    }

    // ════════════════════════════════════════════════════════════
    // ③ 领券
    // ════════════════════════════════════════════════════════════
    @PostMapping("/{couponId}/receive")
    public Result<Void> receive(
            @PathVariable Long couponId,
            @RequestHeader("X-User-Id") Long userId
    ) {
        couponService.receive(userId, couponId);
        return Result.success(null);
    }

    // ════════════════════════════════════════════════════════════
    // ④ 我的券
    // ════════════════════════════════════════════════════════════
    @GetMapping("/mine")
    public Result<List<UserCouponVO>> mine(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(couponService.listMine(userId));
    }

    // ════════════════════════════════════════════════════════════
    // ⑤ Feign 用券 (internal)
    // ════════════════════════════════════════════════════════════
    @PutMapping("/internal/use")
    public Result<BigDecimal> use(@RequestBody UseCouponDTO dto) {
        BigDecimal discount = couponService.useCoupon(
                dto.getUserId(), dto.getUserCouponId(),
                dto.getOrderAmount(), dto.getOrderId());
        return Result.success(discount);
    }

    // ════════════════════════════════════════════════════════════
    // ⑥ Feign 退券 (internal)
    // ════════════════════════════════════════════════════════════
    @PutMapping("/internal/refund/{ucId}")
    public Result<Void> refund(@PathVariable Long ucId) {
        couponService.refundCoupon(ucId);
        return Result.success(null);
    }
}
