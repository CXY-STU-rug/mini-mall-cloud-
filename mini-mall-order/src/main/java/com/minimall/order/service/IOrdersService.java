package com.minimall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.minimall.order.dto.CreateOrderDTO;
import com.minimall.order.dto.ShipOrderDTO;
import com.minimall.order.entity.Orders;
import com.minimall.order.vo.OrderDetailVO;
import com.minimall.order.vo.OrderListVO;

import java.util.List;
import java.util.Map;

/**
 * 订单服务接口 (从单体 IOrdersService 搬, 改: 所有方法第一参数加 Long userId)
 *
 * ════════════════════════════════════════════════════════════════
 * vs 单体差异:
 *   ① 单体的方法签名不带 userId, 用 UserContext.getUserId() 取
 *   ② 微服务里【显式】传 userId 参数, 因为:
 *      - controller 从 @RequestHeader("X-User-Id") 拿到
 *      - service 层不能依赖 ThreadLocal (后续可能跨线程: 异步, MQ 消费者)
 *   ③ closeOrderByMQ 不带 userId, 因为 MQ 消费者线程没登录用户
 *
 * 6 个方法 + 1 个 (单体的 closeTimeoutOrders 兜底定时任务暂不搬)
 * ════════════════════════════════════════════════════════════════
 */
public interface IOrdersService extends IService<Orders> {

    /** 创建订单 */
    Map<String, Object> createOrder(Long userId, CreateOrderDTO dto);

    /** 我的订单列表 */
    List<OrderListVO> listMyOrders(Long userId);

    /** 订单详情 */
    OrderDetailVO getOrderDetail(Long userId, Long orderId);

    /** 用户手动取消订单 */
    void cancelOrder(Long userId, Long orderId);

    /** 标记已付款 (本地模拟支付, 不接真支付) */
    void payOrder(Long userId, Long orderId);

    /**
     * MQ 消费者关单 (注意: 没 userId 参数, 因为消费者没登录上下文)
     * 关键: 必须【幂等】, 消息可能重复投递
     */
    void closeOrderByMQ(Long orderId);

    // ─── G6 物流: 状态机推进 ─────────────────────────────────
    // 状态流转: 1 已付款 ──ship──> 2 已发货 ──sign──> 3 已完成
    // ─────────────────────────────────────────────────────────

    /**
     * 发货 (管理员/仓库系统调用, 无 userId)
     *
     * 状态机前置: status 必须 = 1 (已付款), 否则拒绝
     * 副作用: status 改 2, 填 shipTime + logisticsNo + logisticsCompany
     *
     * ⚠ TODO 安全: 当前无 admin 网关守护, 普通用户能调到这个接口.
     *    真生产环境必须挂在 admin 路径下 + RBAC 鉴权 (G10 admin 模块补)
     */
    void shipOrder(Long orderId, ShipOrderDTO dto);

    /**
     * 签收 (用户主动确认收货)
     *
     * 状态机前置: status 必须 = 2 (已发货), 否则拒绝
     * 副作用: status 改 3, 填 finishTime
     * 越权防护: orders.user_id 必须 = 入参 userId, 否则拒绝
     */
    void signOrder(Long userId, Long orderId);
}
