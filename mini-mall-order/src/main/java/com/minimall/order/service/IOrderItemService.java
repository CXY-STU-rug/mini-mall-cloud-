package com.minimall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.minimall.order.entity.OrderItem;

/**
 * 订单明细服务 (纯 MP, 没自定义方法)
 *
 * OrdersServiceImpl 用 saveBatch / list / removeBy 等基础方法.
 * 不单独暴露 controller, 明细永远跟着 Orders 走.
 */
public interface IOrderItemService extends IService<OrderItem> {
}
