package com.minimall.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单明细 Entity (从单体 com.minimall.minimall.entity 搬过来)
 *
 * ════════════════════════════════════════════════════════════════
 * 设计要点:
 *   一个 orders 对应多行 order_item (一对多)
 *
 *   ⭐ 商品字段 (productName/Image/price) 都是【快照】
 *      下单瞬间从 product 服务 (Feign) 拿到值, 冻结进 order_item
 *      商品改名改价后, 已下单的订单展示仍然是原样
 *
 *   ⭐ 没有 @TableLogic
 *      表结构里就没 is_deleted 字段 (schema.sql 第 145-163 行确认)
 *      order_item 永远跟着 orders 走, orders 逻辑删除时 item 不用动
 *
 * vs 单体差异: 包名换 com.minimall.order.entity, 其余完全一致
 * ════════════════════════════════════════════════════════════════
 */
@Getter
@Setter
@TableName("order_item")
public class OrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 订单 ID (指向 orders.id) */
    private Long orderId;

    /** 商品 ID */
    private Long productId;

    /** 商品名 (快照, 下单时从 product 服务拷过来) */
    private String productName;

    /** 商品图 URL (快照) */
    private String productImage;

    /** 成交单价 (快照, 防止商品改价影响历史订单) */
    private BigDecimal price;

    /** 购买数量 */
    private Integer quantity;

    /** 小计 = price × quantity */
    private BigDecimal subtotal;

    private LocalDateTime createTime;
}
