package com.minimall.order.controller;

import com.minimall.common.core.domain.Result;
import com.minimall.order.dto.CreateOrderDTO;
import com.minimall.order.service.IOrdersService;
import com.minimall.order.vo.OrderDetailVO;
import com.minimall.order.vo.OrderListVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 订单 Controller (G3.7 - 从单体搬, 改 UserContext → X-User-Id header)
 *
 * 端点 (网关代理 /order/** → mini-mall-order):
 *   POST   /order                 → 创建订单 (发延迟 MQ)
 *   GET    /order/my              → 我的订单列表
 *   GET    /order/{orderId}       → 订单详情
 *   PUT    /order/{orderId}/cancel → 用户取消
 *   POST   /order/{orderId}/pay   → 标记付款 (本地模拟)
 *
 * 注: 路径 /order 而不是单体的 /api/order, 跟网关路由 /order/** 对齐.
 */
@RestController
@RequestMapping("/order")
public class OrdersController {

    @Autowired
    private IOrdersService ordersService;

    /** ① 创建订单 */
    @PostMapping
    public Result<Map<String, Object>> create(
            @RequestBody CreateOrderDTO dto,
            @RequestHeader("X-User-Id") Long userId
    ) {
        return Result.success(ordersService.createOrder(userId, dto));
    }

    /** ② 我的订单列表 */
    @GetMapping("/my")
    public Result<List<OrderListVO>> myOrders(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(ordersService.listMyOrders(userId));
    }

    /** ③ 订单详情 */
    @GetMapping("/{orderId}")
    public Result<OrderDetailVO> detail(
            @PathVariable Long orderId,
            @RequestHeader("X-User-Id") Long userId
    ) {
        return Result.success(ordersService.getOrderDetail(userId, orderId));
    }

    /** ④ 取消订单 */
    @PutMapping("/{orderId}/cancel")
    public Result<Void> cancel(
            @PathVariable Long orderId,
            @RequestHeader("X-User-Id") Long userId
    ) {
        ordersService.cancelOrder(userId, orderId);
        return Result.success();
    }

    /** ⑤ 标记付款 (本地模拟) */
    @PostMapping("/{orderId}/pay")
    public Result<Void> pay(
            @PathVariable Long orderId,
            @RequestHeader("X-User-Id") Long userId
    ) {
        ordersService.payOrder(userId, orderId);
        return Result.success();
    }
}
