package com.minimall.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
// ⭐ 改 cart 重复加购 bug: 去掉 @TableLogic, 让 removeById 物理删除
//    根因详见单体 CartItem 同步改动 / E 阶段笔记 chapter 45
// import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 购物车 Entity (从单体 com.minimall.minimall.entity 搬过来)
 *
 * 跨服务的一个细节:
 *   user_id 是【userId】, 不是外键约束。微服务里【跨服务不能有外键】,
 *   因为 user 和 cart 在两个 DB 实例时, 外键会指向不存在的库。
 *   实际生产环境每个微服务都【自己的库】, 这里教学用一个库简化。
 */
@Getter
@Setter
@TableName("cart_item")   // ⭐ 显式指定表名: cart_item (entity 是 CartItem, 默认 cart_item 也行, 写出来清晰)
public class CartItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户 ID (来自 X-User-Id header) */
    private Long userId;

    /** 商品 ID (用来 Feign 调 product 服务拿详情) */
    private Long productId;

    /** 数量 (>0; 0 等于删除) */
    private Integer quantity;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /**
     * 逻辑删除字段 (保留映射, 但已停用 MP 逻辑删除)
     *
     * 改造原因:
     *   原 @TableLogic → removeById 翻译成 UPDATE is_deleted=1
     *   但唯一索引 uk_user_product(user_id, product_id) 不包含 is_deleted,
     *   留下的 is_deleted=1 死行会卡住下次 INSERT (Duplicate entry)
     *
     * 现在:
     *   注解删了 → removeById 翻译成 DELETE FROM cart_item WHERE id=?
     *   购物车是临时数据, 不需要保留历史, 物理删除更干净
     *
     * 为什么字段还在:
     *   表里 is_deleted 列还在, entity 不映射会出兼容问题, 字段保留无害
     */
    private Byte isDeleted;
}
