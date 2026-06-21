package com.minimall.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户领的具体券 (G8 新增)
 * <p>
 * 数据库 UNIQUE KEY (user_id, coupon_id): 每人每种券只能领 1 张.
 * 重复领取在应用层抛"已领取过", DB UNIQUE 是兜底.
 */
@Getter
@Setter
@TableName("user_coupon")
public class UserCoupon implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 领券人 */
    private Long userId;

    /** 关联 coupon.id */
    private Long couponId;

    /** 状态 0=未用 1=已用 (过期看 coupon.valid_to 即可) */
    private Byte status;

    /** 领取时间 */
    private LocalDateTime receiveTime;

    /** 使用时间 (用券时填) */
    private LocalDateTime useTime;

    /** 用在哪个订单上 (退券时核对用) */
    private Long orderId;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Byte isDeleted;
}
