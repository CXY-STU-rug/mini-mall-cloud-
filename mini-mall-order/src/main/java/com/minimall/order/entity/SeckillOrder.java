package com.minimall.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀订单 Entity (从单体搬, 包名改 com.minimall.order.entity)
 *
 * vs 普通 Orders 差异 (为啥单独一张表):
 *   ① 秒杀订单【生成方式不同】: 异步通过 MQ Listener 生成, 不走 createOrder
 *   ② 字段更精简: 没有 receiver/phone/address (秒杀往往是绑定地址或后续补)
 *   ③ 关联 seckill_activity_id (能反查活动详情)
 *
 * 保留 @TableLogic (秒杀订单可逻辑删除, 不像 cart_item 有唯一索引冲突)
 */
@Data
@TableName("seckill_order")
public class SeckillOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 订单号 (格式同普通订单: yyyyMMddHHmmss + userId + 4 位随机) */
    private String orderNo;

    /** 用户 ID */
    private Long userId;

    /** 秒杀活动 ID (反查活动详情) */
    private Long seckillActivityId;

    /** 商品 ID (跨服务用 Feign 查 product 详情) */
    private Long productId;

    /** 状态: 0 待支付 / 1 已支付 / 2 已发货 / 3 已完成 / 4 已取消 */
    private Byte status;

    /** 秒杀价 (快照, 从 activity 拷过来) */
    private BigDecimal seckillPrice;

    private LocalDateTime payTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 逻辑删除. 单体用 Integer, 跟普通 Orders 的 Byte 不一致, 我们保留 Integer 跟表对齐 */
    @TableLogic
    private Integer isDeleted;
}
