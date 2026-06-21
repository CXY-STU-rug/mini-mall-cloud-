package com.minimall.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券模板 (G8 新增)
 * <p>
 * 一行就是一种券, 比如"满100减10".
 * 用户领取后会生成 user_coupon 表的一行【具体券】.
 * <p>
 * 跟 Address 一样用 @Getter/@Setter 风格 (user 服务统一不用 @Data).
 */
@Getter
@Setter
@TableName("coupon")
public class Coupon implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 券名 如"满100减10" */
    private String name;

    /** 类型 1=满减 2=折扣 (G8 主做满减, 折扣留扩展) */
    private Byte type;

    /** 使用门槛 满多少 (订单金额 >= threshold 才可用) */
    private BigDecimal threshold;

    /** 抵扣值 type=1 是金额(10.00) type=2 是折扣率(0.90) */
    private BigDecimal discount;

    /** 总发行量 */
    private Integer totalStock;

    /** 剩余可领数 (领券时原子扣减, 防超发) */
    private Integer remainStock;

    /** 生效时间 */
    private LocalDateTime validFrom;

    /** 过期时间 */
    private LocalDateTime validTo;

    /** 状态 0下架 1上架 */
    private Byte status;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Byte isDeleted;
}
