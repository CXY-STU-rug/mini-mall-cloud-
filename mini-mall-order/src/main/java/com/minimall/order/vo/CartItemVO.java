package com.minimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 购物车项 VO (View Object, 返给前端的数据形状)
 *
 * 为啥单独建 VO?
 *   ① 前端要的是【组合数据】: cart 自己的 id/quantity + 商品的 name/price/img
 *      返 Entity 字段不够 (没有商品名/价格), 直接 join 又跨服务做不到
 *   ② 把展示用的字段集中在一个对象里, 接口干净
 *   ③ 跟 Entity 解耦: Entity 字段加减不影响前端契约
 */
@Data
public class CartItemVO {

    /** 购物车项 id (前端改数量/删除时用) */
    private Long cartItemId;

    /** 商品 id */
    private Long productId;

    /** 商品名 (来自 product 服务 Feign 调用) */
    private String productName;

    /** 商品图 (来自 product 服务) */
    private String productImage;

    /** 当前单价 (实时, 不是下单时快照) */
    private BigDecimal price;

    /** 数量 */
    private Integer quantity;

    /** 小计 = price × quantity */
    private BigDecimal subtotal;
}
