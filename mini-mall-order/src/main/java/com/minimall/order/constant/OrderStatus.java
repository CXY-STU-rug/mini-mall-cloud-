package com.minimall.order.constant;

/**
 * 订单状态常量 (从单体 com.minimall.minimall.common.common.constant 搬过来)
 *
 * 为啥用 byte 不用 enum:
 *   ① 跟数据库 orders.status TINYINT 字段一一对应, 无需转换
 *   ② JSON 序列化简单 (0/1/2/3/4 而不是 "PAID")
 *   ③ enum 跨服务 RPC 序列化容易踩坑
 * 不过缺点是: 编译期不强类型, 但用常量名屏蔽了魔法数
 *
 * 状态机:
 *   UNPAID (0) ─ pay  ───────▶ PAID (1) ─ ship ─▶ SHIPPED (2) ─ done ─▶ COMPLETED (3)
 *      │
 *      ├ cancel ─▶ CANCELLED (4)   (用户手动取消)
 *      └ 30 min TTL ─▶ CANCELLED (4)   (MQ 自动关单)
 */
public class OrderStatus {
    public static final byte UNPAID    = 0;   // 待付款
    public static final byte PAID      = 1;   // 已付款
    public static final byte SHIPPED   = 2;   // 已发货
    public static final byte COMPLETED = 3;   // 已完成
    public static final byte CANCELLED = 4;   // 已取消
}
