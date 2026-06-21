package com.minimall.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀活动 Entity (从单体搬, 包名改 com.minimall.order.entity)
 *
 * 表结构: seckill_activity
 *   id / product_id / seckill_price / stock / status (0待开始/1进行中/2已结束)
 *   start_time / end_time / create_time
 *
 * 跨服务说明:
 *   product_id 不做外键 (微服务跨库无法用 FK), 用 Feign 查商品名/图等
 *
 * 没有 @TableLogic:
 *   秒杀活动一般是【物理删除】或不删 (有 status=2 表示已结束就够)
 *   schema 里 seckill_activity 也没 is_deleted 字段
 */
@Data
@TableName("seckill_activity")
public class SeckillActivity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 商品 ID (跨服务用 Feign 查 product 详情) */
    private Long productId;

    /** 秒杀单价 (必须 < 商品原价) */
    private BigDecimal seckillPrice;

    /** 秒杀库存 (启动后会同步进 Redis, Lua 操作 Redis 而非这字段) */
    private Integer stock;

    /** 状态: 0 待开始 / 1 进行中 / 2 已结束 */
    private Byte status;

    /** 秒杀开始时间 */
    private LocalDateTime startTime;

    /** 秒杀结束时间 */
    private LocalDateTime endTime;

    private LocalDateTime createTime;
}
