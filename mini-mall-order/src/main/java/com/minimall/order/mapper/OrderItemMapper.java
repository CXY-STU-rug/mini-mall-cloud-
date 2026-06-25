package com.minimall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimall.order.entity.OrderItem;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 订单明细 Mapper
 *
 * ADMIN.6 加: 热销 Top 5 聚合.
 *
 * 关键决策: 必须 JOIN orders 过滤掉已取消订单 (status=4) 的 items,
 *           否则取消单也会算进销量, 失真.
 */
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    /**
     * 热销商品 Top 5
     *
     * 业务定义: 销量 = 已付款(1) + 已发货(2) + 已完成(3) 状态订单的累计 quantity
     *            不算 取消(4) 和 待付款(0)
     *
     * JOIN orders 是为了拿到 orders.status 过滤
     * 用 MAX(product_name) 是因为 GROUP BY product_id 时不能裸取 product_name
     *   (实际上同一 product_id 的所有 item.product_name 一致, MAX/MIN 都行)
     */
    @Select("SELECT oi.product_id AS productId, " +
            "       MAX(oi.product_name) AS productName, " +
            "       SUM(oi.quantity) AS totalQty, " +
            "       SUM(oi.subtotal) AS totalSales " +
            "FROM order_item oi " +
            "JOIN orders o ON o.id = oi.order_id " +
            "WHERE o.is_deleted = 0 AND o.status IN (1, 2, 3) " +
            "GROUP BY oi.product_id " +
            "ORDER BY totalQty DESC " +
            "LIMIT 5")
    List<Map<String, Object>> topSellingProducts();
}
