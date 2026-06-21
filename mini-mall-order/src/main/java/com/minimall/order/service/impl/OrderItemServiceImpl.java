package com.minimall.order.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.minimall.order.entity.OrderItem;
import com.minimall.order.mapper.OrderItemMapper;
import com.minimall.order.service.IOrderItemService;
import org.springframework.stereotype.Service;

/**
 * 订单明细服务实现 (空类, 全靠继承的 ServiceImpl 提供 CRUD)
 *
 * @Service 必须有, Spring 才能 @Autowired 注入到 OrdersServiceImpl.
 */
@Service
public class OrderItemServiceImpl
        extends ServiceImpl<OrderItemMapper, OrderItem>
        implements IOrderItemService {
}
