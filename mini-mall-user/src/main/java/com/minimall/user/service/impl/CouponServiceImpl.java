package com.minimall.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.minimall.common.core.exception.BusinessException;
import com.minimall.user.entity.Coupon;
import com.minimall.user.entity.UserCoupon;
import com.minimall.user.mapper.CouponMapper;
import com.minimall.user.mapper.UserCouponMapper;
import com.minimall.user.service.ICouponService;
import com.minimall.user.vo.UserCouponVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 优惠券 Service 实现 (G8 核心)
 * <p>
 * 7 个方法:
 *   ① createCoupon   - 管理: 创建券模板, total_stock 同步给 remain_stock
 *   ② listAvailable  - 用户: 列当前可领的券
 *   ③ receive        - ★ 用户: 领券 (CAS 扣库存 + INSERT user_coupon)
 *   ④ listMine       - 用户: 我的券, JOIN coupon 模板算 expired
 *   ⑤ useCoupon      - ★ Feign: 下单时用券, 校验 + UPDATE + 返抵扣金额
 *   ⑥ refundCoupon   - Feign: 取消订单退券
 */
@Service
public class CouponServiceImpl
        extends ServiceImpl<CouponMapper, Coupon>
        implements ICouponService {

    private static final Logger log = LoggerFactory.getLogger(CouponServiceImpl.class);

    /** 券类型: 满减 */
    private static final byte TYPE_FULL_REDUCTION = 1;

    /** user_coupon 状态: 未用/已用 */
    private static final byte STATUS_UNUSED = 0;
    private static final byte STATUS_USED   = 1;

    @Autowired private CouponMapper couponMapper;
    @Autowired private UserCouponMapper userCouponMapper;

    // ════════════════════════════════════════════════════════════
    // ① 创建券模板
    // ════════════════════════════════════════════════════════════
    @Override
    public Long createCoupon(Coupon coupon) {
        // 教学简化: 不做严格校验, 假设管理端传值合规
        coupon.setRemainStock(coupon.getTotalStock());  // 初始剩余 = 总量
        if (coupon.getStatus() == null) coupon.setStatus((byte) 1);
        save(coupon);
        log.info("[Coupon] 创建券模板 id={} name={}", coupon.getId(), coupon.getName());
        return coupon.getId();
    }

    // ════════════════════════════════════════════════════════════
    // ② 列当前可领的券
    // ════════════════════════════════════════════════════════════
    @Override
    public List<Coupon> listAvailable() {
        LocalDateTime now = LocalDateTime.now();
        return couponMapper.selectList(
                new LambdaQueryWrapper<Coupon>()
                        .eq(Coupon::getStatus, (byte) 1)
                        .gt(Coupon::getRemainStock, 0)
                        .le(Coupon::getValidFrom, now)        // valid_from <= now
                        .ge(Coupon::getValidTo,   now)        // valid_to   >= now
                        .orderByDesc(Coupon::getCreateTime)
        );
    }

    // ════════════════════════════════════════════════════════════
    // ③ 领券 (★ 核心)
    // ════════════════════════════════════════════════════════════
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void receive(Long userId, Long couponId) {

        // ─── Step 1: 校验券存在 + 上架 + 在有效期 ───
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null || coupon.getStatus() != 1) {
            throw new BusinessException("券不存在或已下架");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getValidFrom()) || now.isAfter(coupon.getValidTo())) {
            throw new BusinessException("券不在有效期内");
        }

        // ─── Step 2: 校验该用户没领过 (应用层第一道) ───
        long had = userCouponMapper.selectCount(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, userId)
                        .eq(UserCoupon::getCouponId, couponId)
        );
        if (had > 0) {
            throw new BusinessException("您已领取过该券");
        }

        // ─── Step 3: 原子扣减剩余库存 (CAS) ───
        int rows = couponMapper.deductRemainStock(couponId);
        if (rows == 0) {
            throw new BusinessException("券已领完");
        }

        // ─── Step 4: INSERT user_coupon ───
        // 数据库 UNIQUE KEY (user_id, coupon_id) 是兜底:
        //   高并发下 Step 2 可能漏掉一个, 此处 INSERT 会 DuplicateKeyException
        //   Spring 事务自动回滚, 之前扣的 stock 会回滚 (因为是同事务)
        UserCoupon uc = new UserCoupon();
        uc.setUserId(userId);
        uc.setCouponId(couponId);
        uc.setStatus(STATUS_UNUSED);
        // receive_time / create_time / update_time 走 DB 默认
        userCouponMapper.insert(uc);

        log.info("[Coupon] 领券成功 userId={} couponId={} userCouponId={}",
                 userId, couponId, uc.getId());
    }

    // ════════════════════════════════════════════════════════════
    // ④ 我的券 (含 expired 标志)
    // ════════════════════════════════════════════════════════════
    @Override
    public List<UserCouponVO> listMine(Long userId) {
        // Step 1: 查我所有的 user_coupon
        List<UserCoupon> ucs = userCouponMapper.selectList(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, userId)
                        .orderByDesc(UserCoupon::getReceiveTime)
        );
        if (ucs.isEmpty()) return Collections.emptyList();

        // Step 2: 批量查关联的 coupon 模板 (避免 N+1 查询)
        List<Long> couponIds = ucs.stream().map(UserCoupon::getCouponId).distinct().collect(Collectors.toList());
        Map<Long, Coupon> couponMap = couponMapper.selectBatchIds(couponIds).stream()
                .collect(Collectors.toMap(Coupon::getId, c -> c));

        // Step 3: 组装 VO + 算 expired
        LocalDateTime now = LocalDateTime.now();
        return ucs.stream().map(uc -> {
            UserCouponVO vo = new UserCouponVO();
            BeanUtils.copyProperties(uc, vo);   // 拷 id/userId/couponId/status/receiveTime
            Coupon c = couponMap.get(uc.getCouponId());
            if (c != null) {
                vo.setName(c.getName());
                vo.setType(c.getType());
                vo.setThreshold(c.getThreshold());
                vo.setDiscount(c.getDiscount());
                vo.setValidFrom(c.getValidFrom());
                vo.setValidTo(c.getValidTo());
                vo.setExpired(now.isAfter(c.getValidTo()));
            }
            return vo;
        }).collect(Collectors.toList());
    }

    // ════════════════════════════════════════════════════════════
    // ⑤ 用券 (Feign internal, ★ 核心)
    // ════════════════════════════════════════════════════════════
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal useCoupon(Long userId, Long userCouponId, BigDecimal orderAmount, Long orderId) {

        // ─── 1. 查用户具体券 ───
        UserCoupon uc = userCouponMapper.selectById(userCouponId);
        if (uc == null) {
            throw new BusinessException("券不存在");
        }
        // ─── 2. 越权校验 (防张三用李四的券) ───
        if (!uc.getUserId().equals(userId)) {
            throw new BusinessException("无权使用该券");
        }
        // ─── 3. 状态校验 ───
        if (uc.getStatus() != STATUS_UNUSED) {
            throw new BusinessException("该券已被使用");
        }

        // ─── 4. 查模板算门槛 + 算抵扣金额 ───
        Coupon c = couponMapper.selectById(uc.getCouponId());
        if (c == null) {
            throw new BusinessException("券模板不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(c.getValidTo())) {
            throw new BusinessException("券已过期");
        }
        if (orderAmount.compareTo(c.getThreshold()) < 0) {
            throw new BusinessException("订单金额未达到使用门槛 " + c.getThreshold() + " 元");
        }

        // type=1 满减: 抵扣 = c.discount
        // type=2 折扣: 抵扣 = orderAmount * (1 - c.discount), G8 教学先不做
        BigDecimal discountAmount = c.getDiscount();

        // ─── 5. UPDATE user_coupon 标记已用 ───
        // 用 LambdaUpdateWrapper 是为了【条件 status=0】, 防并发被两个订单都用了
        int rows = userCouponMapper.update(
                null,
                new LambdaUpdateWrapper<UserCoupon>()
                        .eq(UserCoupon::getId, userCouponId)
                        .eq(UserCoupon::getStatus, STATUS_UNUSED)
                        .set(UserCoupon::getStatus, STATUS_USED)
                        .set(UserCoupon::getUseTime, now)
                        .set(UserCoupon::getOrderId, orderId)
        );
        if (rows == 0) {
            // CAS 失败: 并发下被人抢用了
            throw new BusinessException("券正在被使用, 请稍后再试");
        }

        log.info("[Coupon] 用券 userId={} ucId={} discount={} orderId={}",
                 userId, userCouponId, discountAmount, orderId);
        return discountAmount;
    }

    // ════════════════════════════════════════════════════════════
    // ⑥ 退券 (Feign internal)
    // ════════════════════════════════════════════════════════════
    @Override
    public void refundCoupon(Long userCouponId) {
        UserCoupon uc = userCouponMapper.selectById(userCouponId);
        if (uc == null) {
            // 教学: 退一个不存在的券, log 一下就好, 不抛 (退券应该幂等)
            log.warn("[Coupon] refund 找不到券 ucId={}", userCouponId);
            return;
        }
        if (uc.getStatus() == STATUS_UNUSED) {
            // 已经是未用状态, 幂等返回
            return;
        }

        userCouponMapper.update(
                null,
                new LambdaUpdateWrapper<UserCoupon>()
                        .eq(UserCoupon::getId, userCouponId)
                        .set(UserCoupon::getStatus, STATUS_UNUSED)
                        .set(UserCoupon::getUseTime, null)
                        .set(UserCoupon::getOrderId, null)
        );
        log.info("[Coupon] 退券 ucId={}", userCouponId);
    }
}
