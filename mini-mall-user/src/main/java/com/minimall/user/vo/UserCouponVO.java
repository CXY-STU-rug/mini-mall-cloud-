package com.minimall.user.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * "我的优惠券"展示 VO
 * <p>
 * 把 UserCoupon 跟它对应的 Coupon 模板合并展示, 前端只看一个对象.
 * 加一个 expired 字段, Service 算好 "是否过期" 给前端用 (不用前端比时间).
 */
@Data
public class UserCouponVO {

    /** user_coupon 主键 */
    private Long id;
    /** 关联的券模板 id */
    private Long couponId;
    /** 状态 0=未用 1=已用 */
    private Byte status;
    /** 是否过期 (Service 比 NOW vs coupon.valid_to 算出) */
    private Boolean expired;

    // ─── 从 coupon 模板带过来的展示字段 ───
    private String name;
    private Byte type;
    private BigDecimal threshold;
    private BigDecimal discount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime validFrom;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime validTo;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime receiveTime;
}
