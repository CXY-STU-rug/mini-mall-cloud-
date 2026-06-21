package com.minimall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimall.order.entity.OrderItem;

/**
 * 订单明细 Mapper
 *
 * 同 OrdersMapper: 继承 BaseMapper, 走 @MapperScan, 无 XML.
 */
public interface OrderItemMapper extends BaseMapper<OrderItem> {
}
