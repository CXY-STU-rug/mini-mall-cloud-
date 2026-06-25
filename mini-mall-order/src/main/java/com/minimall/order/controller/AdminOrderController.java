package com.minimall.order.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minimall.common.core.domain.Result;
import com.minimall.common.core.exception.BusinessException;
import com.minimall.order.dto.AdminOrderPageDTO;
import com.minimall.order.dto.ShipOrderDTO;
import com.minimall.order.entity.Orders;
import com.minimall.order.mapper.OrderItemMapper;
import com.minimall.order.mapper.OrdersMapper;
import com.minimall.order.service.IOrdersService;
import com.minimall.order.vo.OrderDetailVO;
import com.minimall.order.vo.OrderStatsVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台订单管理 Controller (ADMIN.5)
 *
 * 端点设计:
 *   GET    /admin/order/page             分页 + 状态 / 用户 / 关键词筛选
 *   GET    /admin/order/{id}             详情 (含订单项 + 物流 + 收货信息)
 *   PUT    /admin/order/{id}/ship        发货 (1 已付款 → 2 已发货, 填物流号)
 *   PUT    /admin/order/{id}/close       关闭未付款订单 (0 → 4)
 *
 * 设计取舍:
 *   ① 没有 "编辑订单字段" — 订单是用户产生的【事实记录】, 不能瞎改
 *   ② 没有 "删除订单"     — 软删意义不大, 直接 close 走业务流程
 *   ③ 发货 / 关闭 都【复用】用户端的 service 方法, 走同一套状态机校验
 *
 * 鉴权: 网关 AuthGlobalFilter 已校验 role=1, 这里不需要再写
 */
@RestController
@RequestMapping("/admin/order")
public class AdminOrderController {

    @Autowired
    private IOrdersService ordersService;

    @Autowired
    private OrdersMapper ordersMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    /** 状态码 → 中文 (跟前端 ORDER_STATUS_LABEL 保持一致) */
    private static final String[] STATUS_LABELS = {"待付款", "已付款", "已发货", "已完成", "已取消"};

    // ════════════════════════════════════════════════════════════
    // ① 分页查询
    // ════════════════════════════════════════════════════════════
    @GetMapping("/page")
    public Result<IPage<Orders>> page(AdminOrderPageDTO query) {
        Page<Orders> p = new Page<>(query.getPage(), query.getSize());

        LambdaQueryWrapper<Orders> w = new LambdaQueryWrapper<>();
        w.eq(query.getStatus() != null, Orders::getStatus, query.getStatus());
        w.eq(query.getUserId() != null, Orders::getUserId, query.getUserId());

        // 关键词模糊匹配多个字段: orderNo OR receiver OR phone
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            String kw = query.getKeyword().trim();
            w.and(q -> q
                    .like(Orders::getOrderNo, kw)
                    .or().like(Orders::getReceiver, kw)
                    .or().like(Orders::getPhone, kw)
            );
        }

        w.orderByDesc(Orders::getId);   // 新订单在前
        return Result.success(ordersService.page(p, w));
    }

    // ════════════════════════════════════════════════════════════
    // ② 详情 (跳过越权, admin 能看任何用户的订单)
    // ════════════════════════════════════════════════════════════
    @GetMapping("/{id}")
    public Result<OrderDetailVO> detail(@PathVariable Long id) {
        return Result.success(ordersService.getOrderDetailForAdmin(id));
    }

    // ════════════════════════════════════════════════════════════
    // ③ 发货 (复用现有 shipOrder, 它已无 userId 校验)
    // ════════════════════════════════════════════════════════════
    @PutMapping("/{id}/ship")
    public Result<Void> ship(@PathVariable Long id,
                             @RequestBody @Valid ShipOrderDTO dto) {
        // service.shipOrder 内部已校验:
        //   - 订单存在
        //   - status == PAID(1), 否则抛异常
        //   - 改 status=2 + 填 shipTime / logisticsNo / logisticsCompany
        ordersService.shipOrder(id, dto);
        return Result.success();
    }

    // ════════════════════════════════════════════════════════════
    // ④ 关闭未付款订单 (admin 介入, 取出原 userId 调 cancelOrder)
    // ════════════════════════════════════════════════════════════
    @PutMapping("/{id}/close")
    public Result<Void> close(@PathVariable Long id) {
        Orders order = ordersService.getById(id);
        if (order == null) throw new BusinessException(404, "订单不存在");

        // 复用用户取消逻辑: 内部锁 + 事务 + 状态机校验 (必须 UNPAID) + 回库存
        // 传入订单原 userId 是为了通过 cancelOrder 内的 userId 一致校验
        ordersService.cancelOrder(order.getUserId(), id);
        return Result.success();
    }

    // ════════════════════════════════════════════════════════════
    // ⑤ ADMIN.6 看板: 一次性返所有订单维度聚合
    // ════════════════════════════════════════════════════════════
    @GetMapping("/stats")
    public Result<OrderStatsVO> stats() {
        OrderStatsVO vo = new OrderStatsVO();

        // 总订单数 (用 BaseMapper.selectCount + null wrapper 走全表 count)
        vo.setTotalOrders(ordersMapper.selectCount(null));
        vo.setTotalGmv(ordersMapper.sumCompletedGmv());
        vo.setTodayOrders(ordersMapper.countTodayOrders());

        // ─── 状态分布: 5 桶补 0 ──────────────────────────────
        // SQL 只返有数据的桶, 比如全是 status=4, 那 0/1/2/3 桶都不返
        // 这里补齐: 5 个状态都有, count=0 表示无数据
        Map<Byte, Long> rawStatus = new HashMap<>();
        for (Map<String, Object> row : ordersMapper.countByStatus()) {
            Byte st = ((Number) row.get("status")).byteValue();
            Long cnt = ((Number) row.get("cnt")).longValue();
            rawStatus.put(st, cnt);
        }
        List<OrderStatsVO.StatusCount> statusDist = new ArrayList<>();
        for (byte s = 0; s <= 4; s++) {
            OrderStatsVO.StatusCount sc = new OrderStatsVO.StatusCount();
            sc.setStatus(s);
            sc.setLabel(STATUS_LABELS[s]);
            sc.setCount(rawStatus.getOrDefault(s, 0L));
            statusDist.add(sc);
        }
        vo.setStatusDist(statusDist);

        // ─── 7 天趋势: 日期补全 ──────────────────────────────
        // SQL 只返有数据的日期, 这里把 7 天每天都列出, 没数据的补 0
        Map<String, Long> rawDaily = new HashMap<>();
        for (Map<String, Object> row : ordersMapper.countDaily7d()) {
            // MySQL 的 DATE() 返 java.sql.Date, toString 是 "yyyy-MM-dd"
            String day = row.get("day").toString();
            Long cnt = ((Number) row.get("cnt")).longValue();
            rawDaily.put(day, cnt);
        }
        List<OrderStatsVO.DailyCount> daily = new ArrayList<>();
        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
        for (int i = 6; i >= 0; i--) {                  // 6 天前 ~ 今天
            LocalDate d = today.minusDays(i);
            String key = d.format(fmt);
            OrderStatsVO.DailyCount dc = new OrderStatsVO.DailyCount();
            dc.setDate(key);
            dc.setCount(rawDaily.getOrDefault(key, 0L));
            daily.add(dc);
        }
        vo.setDailyOrders(daily);

        // ─── 热销 Top 5 (JOIN orders 过滤掉取消单后的销量) ─────
        List<OrderStatsVO.TopProductVO> tops = new ArrayList<>();
        for (Map<String, Object> row : orderItemMapper.topSellingProducts()) {
            OrderStatsVO.TopProductVO tp = new OrderStatsVO.TopProductVO();
            tp.setProductId(((Number) row.get("productId")).longValue());
            tp.setProductName(row.get("productName").toString());
            tp.setTotalQty(((Number) row.get("totalQty")).longValue());
            tp.setTotalSales(new BigDecimal(row.get("totalSales").toString()));
            tops.add(tp);
        }
        vo.setTopProducts(tops);

        return Result.success(vo);
    }
}
