package com.minimall.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.minimall.common.core.domain.Result;
import com.minimall.common.core.exception.BusinessException;
import com.minimall.order.client.ProductFeignClient;
import com.minimall.order.client.UserFeignClient;
import com.minimall.order.config.RabbitMQConfig;
import com.minimall.order.constant.OrderStatus;
import com.minimall.order.dto.CreateOrderDTO;
import com.minimall.order.dto.ShipOrderDTO;
import com.minimall.order.entity.CartItem;
import com.minimall.order.entity.OrderItem;
import com.minimall.order.entity.Orders;
import com.minimall.order.mapper.OrderItemMapper;
import com.minimall.order.mapper.OrdersMapper;
import com.minimall.order.service.ICartItemService;
import com.minimall.order.service.IOrderItemService;
import com.minimall.order.service.IOrdersService;
import com.minimall.order.util.RedisLockUtil;
import com.minimall.order.vo.OrderDetailVO;
import com.minimall.order.vo.OrderItemVO;
import com.minimall.order.vo.OrderListVO;
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 订单服务实现 (G3.7 搬迁 + G3.10 补扣库存 + H1 Feign fallback)
 *
 * ════════════════════════════════════════════════════════════════
 * vs 单体差异:
 *   ① userId 不再读 UserContext, 从【方法参数】传入
 *   ② addressService.getById  → userFeignClient.getAddress  (HTTP)
 *   ③ productService.listByIds → 循环 productFeignClient.getById (HTTP × N)
 *      性能差但教学简单, 生产应给 product 加 batch 接口
 *   ④ 扣/回库存改成 Feign 跨服务调用 productFeignClient.deductStock / restoreStock
 *      ⚠ 跨服务事务局限: order 抛异常时 product 已扣的库存不会自动回滚
 *        (分布式事务问题, 等 Seata/MQ 补偿表解)
 *   ⑤ 商品/地址数据 Map<String, Object>, 因为 order 引不到 Product/Address entity
 *   ⑥ H1: 3 个 Feign Client 都有 fallback, product/user 挂了走兜底
 *
 * 6 个方法分布:
 *   createOrder        ★ 最复杂, 8 步 + 锁 + 事务 + 事务外发 MQ + 跨服务扣库存
 *   listMyOrders       中等, 批量查 + Map 分组
 *   getOrderDetail     简单
 *   cancelOrder        中等, 锁 + 事务 + 状态机 + 回库存
 *   payOrder           中等, 同 cancel 套路 (但不回库存)
 *   closeOrderByMQ     简单 + 幂等, 没用户上下文 + 回库存
 * ════════════════════════════════════════════════════════════════
 */
@Service
public class OrdersServiceImpl extends ServiceImpl<OrdersMapper, Orders> implements IOrdersService {

    // ─── 同服务依赖 (直接 @Autowired) ───
    @Autowired private ICartItemService cartItemService;        // 同服务: cart 在 order 模块
    @Autowired private IOrderItemService orderItemService;
    @Autowired private OrdersMapper ordersMapper;
    @Autowired private OrderItemMapper orderItemMapper;

    // ─── 跨服务依赖 (Feign) ───
    @Autowired private UserFeignClient userFeignClient;         // 查地址
    @Autowired private ProductFeignClient productFeignClient;   // 查商品

    // ─── 基础设施 ───
    @Autowired private RedisLockUtil redisLockUtil;
    @Autowired private TransactionTemplate transactionTemplate; // 控制事务边界 (事务提交后才发 MQ)
    @Autowired private RabbitTemplate rabbitTemplate;

    // ════════════════════════════════════════════════════════════
    // ① 创建订单 (大头, 7 步 + 锁 + 事务 + 事务外发 MQ)
    // ════════════════════════════════════════════════════════════
    @Override
    @GlobalTransactional(name = "createOrder-tx", rollbackFor = Exception.class)
    public Map<String, Object> createOrder(Long userId, CreateOrderDTO dto) {

        // ⭐ G5: @GlobalTransactional 入口
        //   - Seata TM 在这分配全局 XID
        //   - Feign 自动透传 XID 到 product
        //   - product 扣库存写 undo_log, 一阶段本地提交
        //   - 本方法抛任何异常 → TC 反向通知 product 走 undo_log 回滚 stock
        //   - 内部 TransactionTemplate 不动 (它是 order 本地事务,
        //     作为全局事务的一个分支自动管理)

        // 同用户 10 秒内只能下一单 (防重复下单, 防双击)
        String lockKey = "lock:order:user:" + userId;
        String owner = redisLockUtil.tryLock(lockKey, 10);
        if (owner == null) {
            throw new BusinessException(429, "操作太频繁, 请稍后再试");
        }

        try {
            // ⭐ TransactionTemplate 而非 @Transactional 注解:
            //    需要在事务【提交后】发 MQ (防止事务回滚但 MQ 已发出 → 幽灵消息)
            Map<String, Object> orderResult = transactionTemplate.execute(status -> {

                // ─── 第 1 步: 参数校验 ───────────────────
                if (dto.getCartItemIds() == null || dto.getCartItemIds().isEmpty()) {
                    throw new BusinessException(400, "请选择要购买的商品");
                }
                if (dto.getAddressId() == null) {
                    throw new BusinessException(400, "请选择收货地址");
                }

                // ─── 第 2 步: 地址校验 (Feign 调 user 服务) ─
                Result<Map<String, Object>> addrResp = userFeignClient.getAddress(dto.getAddressId(), userId);
                if (addrResp == null || addrResp.getCode() != 200 || addrResp.getData() == null) {
                    throw new BusinessException(403, "收货地址无效");
                }
                Map<String, Object> address = addrResp.getData();
                // user 服务的 detail 接口已经做了"是否你的"校验, Feign 这里不用再判 userId

                // ─── 第 3 步: 购物车校验 (同服务) ───────
                List<CartItem> cartItems = cartItemService.listByIds(dto.getCartItemIds());
                if (cartItems.size() != dto.getCartItemIds().size()) {
                    throw new BusinessException(400, "购物车项不存在");
                }
                for (CartItem ci : cartItems) {
                    if (!ci.getUserId().equals(userId)) {
                        throw new BusinessException(403, "无权操作他人购物车");
                    }
                }

                // ─── 第 4 步: 批量查商品 (Feign 调 product 服务) ─
                // 性能注意: 这里 N 次 HTTP, 生产应给 product 加 batch 接口
                Map<Long, Map<String, Object>> productMap = new HashMap<>();
                for (CartItem ci : cartItems) {
                    Result<Map<String, Object>> pResp = productFeignClient.getById(ci.getProductId());
                    if (pResp == null || pResp.getCode() != 200 || pResp.getData() == null) {
                        throw new BusinessException(400, "商品不存在: id=" + ci.getProductId());
                    }
                    productMap.put(ci.getProductId(), pResp.getData());
                }

                // ─── 第 5 步: 计算总价 + 构造 OrderItem (含商品快照) ─
                BigDecimal totalAmount = BigDecimal.ZERO;
                List<OrderItem> orderItems = new ArrayList<>();

                for (CartItem ci : cartItems) {
                    Map<String, Object> p = productMap.get(ci.getProductId());

                    // 商品状态校验 (status=0 已下架)
                    Object statusObj = p.get("status");
                    if (statusObj != null && Integer.parseInt(statusObj.toString()) == 0) {
                        throw new BusinessException(400, "商品已下架: " + p.get("name"));
                    }

                    // 取价格 (Map 取值要 toString 转 BigDecimal, 防 Double 精度问题)
                    BigDecimal price = new BigDecimal(p.get("price").toString());

                    // 小计 = 单价 × 数量, 必须 .multiply 不能 *
                    BigDecimal subtotal = price.multiply(BigDecimal.valueOf(ci.getQuantity()));
                    totalAmount = totalAmount.add(subtotal);

                    // ⭐ 商品快照 (下单瞬间冻结, 商品改名改价不影响这单)
                    OrderItem oi = new OrderItem();
                    oi.setProductId(ci.getProductId());
                    oi.setProductName((String) p.get("name"));
                    oi.setProductImage((String) p.get("coverImage"));
                    oi.setPrice(price);
                    oi.setQuantity(ci.getQuantity());
                    oi.setSubtotal(subtotal);
                    orderItems.add(oi);
                }

                // 生成订单号: yyyyMMddHHmmss + userId + 4 位随机
                String orderNo = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                        + userId + String.format("%04d", new Random().nextInt(10000));

                // 构造 Orders 主表 (地址快照: 拼省+市+区+详细)
                Orders order = new Orders();
                order.setOrderNo(orderNo);
                order.setUserId(userId);
                order.setTotalAmount(totalAmount);
                order.setStatus(OrderStatus.UNPAID);   // 0 = 待付款
                order.setReceiver((String) address.get("receiver"));
                order.setPhone((String) address.get("phone"));
                order.setAddress("" + address.get("province") + address.get("city")
                        + address.get("district") + address.get("detail"));
                order.setRemark(dto.getRemark());

                this.save(order);   // ⭐ MP 自动回填 id 到 order.id

                // ─── 第 6 步: 批量保存 OrderItem (orderId 现在才有) ─
                for (OrderItem oi : orderItems) {
                    oi.setOrderId(order.getId());
                }
                orderItemService.saveBatch(orderItems);

                // ─── 第 7 步: 扣库存(G3.10 补) ──────────
                // ⭐ 跨服务调 product 服务的原子 SQL UPDATE...WHERE stock>=qty
                //    rows=0 表示 stock 不够, 抛业务异常 → @Transactional 回滚之前 save 的订单/明细
                //    注意: 这里只回滚 order 库的事务, 已扣的 product 库不会自动回滚 (分布式事务问题, 留 Seata 解)
                for (CartItem ci : cartItems) {
                    try {
                        Result<Integer> deductResp = productFeignClient.deductStock(ci.getProductId(), ci.getQuantity());
                        if (deductResp == null || deductResp.getCode() != 200) {
                            Map<String, Object> p = productMap.get(ci.getProductId());
                            throw new BusinessException(400, "库存不足: " + p.get("name"));
                        }
                    } catch (BusinessException e) {
                        throw e;
                    } catch (Exception feignEx) {
                        Map<String, Object> p = productMap.get(ci.getProductId());
                        throw new BusinessException(400, "库存不足: " + p.get("name"));
                    }
                }

                // ─── 第 8 步: 清掉这几条购物车 ─────────
                cartItemService.removeByIds(dto.getCartItemIds());

                // 返回 orderId + orderNo
                Map<String, Object> result = new HashMap<>();
                result.put("orderNo", orderNo);
                result.put("orderId", order.getId());
                return result;
            });

            // ⭐⭐⭐ 事务外发延迟 MQ (G2 通路真用)
            //    放事务里万一回滚, MQ 已发出 → 死信 30 秒后关一个不存在的订单 = 幽灵消息
            //    放事务外 (execute 返回后), 此时事务已提交, 订单已落库, MQ 安全发出
            Long orderId = (Long) orderResult.get("orderId");
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.DELAY_EXCHANGE,        // 延迟交换机
                    RabbitMQConfig.DELAY_ROUTING_KEY,     // routingKey "delay"
                    orderId                                // 消息体: 订单 ID
            );

            return orderResult;

        } finally {
            // 锁必须释放, 否则要等 10 秒 TTL
            redisLockUtil.unlock(lockKey, owner);
        }
    }

    // ════════════════════════════════════════════════════════════
    // ② 我的订单列表 (批量查 + Map 分组组装)
    // ════════════════════════════════════════════════════════════
    @Override
    public List<OrderListVO> listMyOrders(Long userId) {
        // 第 1 步: 查我的所有订单
        QueryWrapper<Orders> ow = new QueryWrapper<>();
        ow.eq("user_id", userId).orderByDesc("create_time");
        List<Orders> orders = this.list(ow);

        if (orders.isEmpty()) {
            return new ArrayList<>();
        }

        // 第 2 步: 收集 orderIds, 批量查所有明细 (SQL: WHERE order_id IN (1,2,3))
        List<Long> orderIds = new ArrayList<>();
        for (Orders o : orders) {
            orderIds.add(o.getId());
        }
        QueryWrapper<OrderItem> iw = new QueryWrapper<>();
        iw.in("order_id", orderIds);
        List<OrderItem> allItems = orderItemService.list(iw);

        // 第 3 步: 按 orderId 分组成 Map<orderId, List<明细>>
        // computeIfAbsent: key 不存在就 put 空 list, 返回 list, 然后直接 .add(item)
        Map<Long, List<OrderItem>> itemMap = new HashMap<>();
        for (OrderItem item : allItems) {
            itemMap.computeIfAbsent(item.getOrderId(), k -> new ArrayList<>()).add(item);
        }

        // 第 4 步: 循环订单组装 VO
        List<OrderListVO> result = new ArrayList<>();
        for (Orders o : orders) {
            OrderListVO vo = new OrderListVO();
            vo.setOrderId(o.getId());
            vo.setOrderNo(o.getOrderNo());
            vo.setStatus(o.getStatus());
            vo.setStatusDesc(statusDesc(o.getStatus()));   // 翻译中文
            vo.setTotalAmount(o.getTotalAmount());
            vo.setReceiver(o.getReceiver());
            vo.setAddress(o.getAddress());
            vo.setCreateTime(o.getCreateTime());

            // 组装明细列表
            List<OrderItem> myItems = itemMap.getOrDefault(o.getId(), new ArrayList<>());
            List<OrderItemVO> itemVOs = new ArrayList<>();
            for (OrderItem item : myItems) {
                OrderItemVO ivo = new OrderItemVO();
                ivo.setOrderItemId(item.getId());
                ivo.setProductId(item.getProductId());
                ivo.setProductName(item.getProductName());
                ivo.setProductImage(item.getProductImage());
                ivo.setPrice(item.getPrice());
                ivo.setQuantity(item.getQuantity());
                ivo.setSubtotal(item.getSubtotal());
                itemVOs.add(ivo);
            }
            vo.setItems(itemVOs);
            result.add(vo);
        }
        return result;
    }

    /** 状态码翻译成中文 (私有辅助) */
    private String statusDesc(Byte status) {
        if (status == null) return "未知";
        switch (status) {
            case 0:  return "待付款";
            case 1:  return "已付款";
            case 2:  return "已发货";
            case 3:  return "已完成";
            case 4:  return "已取消";
            default: return "未知";
        }
    }

    // ════════════════════════════════════════════════════════════
    // ③ 订单详情
    // ════════════════════════════════════════════════════════════
    @Override
    public OrderDetailVO getOrderDetail(Long userId, Long orderId) {
        Orders order = ordersMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权访问该订单");
        }

        // 查明细
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId)
        );

        List<OrderItemVO> itemVOs = new ArrayList<>();
        for (OrderItem item : items) {
            OrderItemVO ivo = new OrderItemVO();
            ivo.setOrderItemId(item.getId());
            ivo.setProductId(item.getProductId());
            ivo.setProductName(item.getProductName());
            ivo.setProductImage(item.getProductImage());
            ivo.setPrice(item.getPrice());
            ivo.setQuantity(item.getQuantity());
            ivo.setSubtotal(item.getSubtotal());
            itemVOs.add(ivo);
        }

        // BeanUtils 拷主表字段 (orderNo/totalAmount/receiver/...)
        OrderDetailVO vo = new OrderDetailVO();
        BeanUtils.copyProperties(order, vo);
        vo.setOrderId(order.getId());     // 主表叫 id, vo 字段叫 orderId, 手动设
        vo.setStatusDesc(statusDesc(order.getStatus()));
        vo.setItems(itemVOs);
        return vo;
    }

    // ════════════════════════════════════════════════════════════
    // ④ 用户手动取消订单
    // ════════════════════════════════════════════════════════════
    @Override
    public void cancelOrder(Long userId, Long orderId) {
        // 订单级锁 (防双击取消)
        String lockKey = "lock:order:cancel:" + orderId;
        String owner = redisLockUtil.tryLock(lockKey, 10);
        if (owner == null) {
            throw new BusinessException(429, "操作太频繁");
        }
        try {
            transactionTemplate.execute(status -> {
                Orders order = ordersMapper.selectById(orderId);
                if (order == null) throw new BusinessException(404, "订单不存在");
                if (!order.getUserId().equals(userId)) throw new BusinessException(403, "无权操作");

                // 状态机: 只有"待付款"才能取消 (幂等保障 - 重复请求会被这里挡)
                if (order.getStatus() != 0) {
                    throw new BusinessException(400, "当前订单状态不可取消");
                }

                order.setStatus(OrderStatus.CANCELLED);
                ordersMapper.updateById(order);

                // ── G3.10 回库存: 查该订单全部明细, 逐个 Feign 调 product 还库存 ──
                QueryWrapper<OrderItem> itemW = new QueryWrapper<>();
                itemW.eq("order_id", orderId);
                List<OrderItem> items = orderItemMapper.selectList(itemW);
                for (OrderItem item : items) {
                    productFeignClient.restoreStock(item.getProductId(), item.getQuantity());
                }
                return null;
            });
        } finally {
            redisLockUtil.unlock(lockKey, owner);
        }
    }

    // ════════════════════════════════════════════════════════════
    // ⑤ 标记已付款 (本地模拟, 不接真支付)
    // ════════════════════════════════════════════════════════════
    @Override
    public void payOrder(Long userId, Long orderId) {
        // ⭐ key 用 orderId 而非 userId: 允许同时支付多个订单, 但同一单只能一次
        String lockKey = "lock:order:pay:" + orderId;
        String owner = redisLockUtil.tryLock(lockKey, 10);
        if (owner == null) {
            throw new BusinessException(429, "操作太频繁");
        }
        try {
            transactionTemplate.execute(status -> {
                Orders order = ordersMapper.selectById(orderId);
                if (order == null) throw new BusinessException(404, "订单不存在");
                if (!order.getUserId().equals(userId)) throw new BusinessException(403, "无权操作");

                // 状态机: 只有 0=待付款 能支付
                if (order.getStatus() != 0) {
                    throw new BusinessException(400, "订单不可支付");
                }

                order.setStatus(OrderStatus.PAID);
                order.setPayTime(LocalDateTime.now());
                ordersMapper.updateById(order);
                return null;
            });
        } finally {
            redisLockUtil.unlock(lockKey, owner);
        }
    }

    // ════════════════════════════════════════════════════════════
    // ⑥ MQ 消费者关单 (注意: 没 userId, 必须幂等)
    // ════════════════════════════════════════════════════════════
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeOrderByMQ(Long orderId) {
        Orders order = ordersMapper.selectById(orderId);
        if (order == null) {
            return;   // 订单不存在 (可能被手动取消并物理删了), 跳过
        }

        // ⭐ 幂等核心: 只有"待付款"才关
        //    用户已付款 → 不能关
        //    用户已手动取消 → 已是 CANCELLED, 跳过
        //    消息重复投递 → 第二次进来这里也是 4, 直接跳过
        if (!order.getStatus().equals(OrderStatus.UNPAID)) {
            return;
        }

        order.setStatus(OrderStatus.CANCELLED);
        ordersMapper.updateById(order);

        // ── G3.10 回库存: 同 cancelOrder 套路 ──
        QueryWrapper<OrderItem> itemW = new QueryWrapper<>();
        itemW.eq("order_id", orderId);
        List<OrderItem> items = orderItemMapper.selectList(itemW);
        for (OrderItem item : items) {
            productFeignClient.restoreStock(item.getProductId(), item.getQuantity());
        }
    }

    // ════════════════════════════════════════════════════════════
    // ⑦ G6 物流: 发货 (管理员调用)
    // ════════════════════════════════════════════════════════════
    @Override
    public void shipOrder(Long orderId, ShipOrderDTO dto) {
        // ─────────────────────────────────────────────────────────
        // TODO 用户填: 状态机 4 步模板 (参考 cancelOrder 行 380~390)
        //
        //   第 1 步: 查订单, 为空抛 BusinessException(404, "订单不存在")
        Orders order = ordersMapper.selectById(orderId);
        if (order == null) throw new BusinessException(404, "订单不存在");
        //   第 2 步: 跳过 user_id 校验 (admin 不需要)
        //   第 3 步: 状态机前置 — 必须 == OrderStatus.PAID (=1)
        //            否则抛 BusinessException(400, "订单状态不可发货")
        if (!order.getStatus().equals(OrderStatus.PAID)) {
            throw new BusinessException(400, "订单状态不可发货");
        }

        //   第 4 步: setStatus(OrderStatus.SHIPPED) + setShipTime(now)
        //            + setLogisticsCompany(dto.getLogisticsCompany())
        //            + setLogisticsNo(dto.getLogisticsNo())
        //            最后 ordersMapper.updateById(order);
        //
        // 提示: now 用 LocalDateTime.now()
        //       OrderStatus.SHIPPED 是已定义的常量 (=2)
        // ─────────────────────────────────────────────────────────
        order.setStatus(OrderStatus.SHIPPED);
        order.setShipTime(LocalDateTime.now());
        order.setLogisticsCompany(dto.getLogisticsCompany());
        order.setLogisticsNo(dto.getLogisticsNo());
        ordersMapper.updateById(order);

    }

    // ════════════════════════════════════════════════════════════
    // ⑧ G6 物流: 签收 (用户主动)
    // ════════════════════════════════════════════════════════════
    @Override
    public void signOrder(Long userId, Long orderId) {
        // ─────────────────────────────────────────────────────────
        // TODO 用户填: 状态机 4 步模板 (参考 cancelOrder)
        //
        //   第 1 步: 查订单, 为空抛 BusinessException(404, "订单不存在")

        //   第 2 步: 越权校验 — order.getUserId().equals(userId)
        //            不匹配抛 BusinessException(403, "无权操作")

        //   第 3 步: 状态机前置 — 必须 == OrderStatus.SHIPPED (=2)
        //            否则抛 BusinessException(400, "订单状态不可签收")

        //   第 4 步: setStatus(OrderStatus.COMPLETED) + setFinishTime(now)
        //            ordersMapper.updateById(order);
        //
        // 提示: OrderStatus.COMPLETED 是已定义的常量 (=3)
        // ─────────────────────────────────────────────────────────
        Orders order = ordersMapper.selectById(orderId);
        if (order == null) throw new BusinessException(404, "订单不存在");

        // 第 2 步: 越权校验 (用户类操作必须做!)
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作");
        }

        // 第 3 步: 状态机前置 — 必须 SHIPPED 才能签收
        if (!order.getStatus().equals(OrderStatus.SHIPPED)) {
            throw new BusinessException(400, "订单状态不可签收");
        }

        // 第 4 步: 改状态 + 填签收时间
        order.setStatus(OrderStatus.COMPLETED);
        order.setFinishTime(LocalDateTime.now());
        ordersMapper.updateById(order);
    }
}

