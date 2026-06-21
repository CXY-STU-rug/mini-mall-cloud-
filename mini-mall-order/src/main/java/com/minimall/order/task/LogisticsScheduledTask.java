package com.minimall.order.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.minimall.order.constant.OrderStatus;
import com.minimall.order.entity.Orders;
import com.minimall.order.mapper.OrdersMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 物流定时任务 (G6.7)
 *
 * 功能: 兜底自动签收
 *   电商行业惯例 — 用户发货后 7 天未点签收, 系统自动签收完单
 *   避免订单卡在"已发货"永远不结清, 影响财务对账 / 售后期计算
 *
 * ⚠ 单机调度局限 (生产必看):
 *   当前用 Spring @Scheduled 内置调度, 是【每实例独立】跑.
 *   如果 order-service 部署 3 个实例 → 3 个实例同时跑 → 同一个订单可能被签收 3 次!
 *
 *   生产方案 (从轻到重):
 *     1. Redis 分布式锁: 任务进来先 SETNX 一把锁, 抢到才跑
 *     2. 选主: 用 Nacos / ZK 选一个实例当 leader, 只有 leader 跑
 *     3. 分布式调度框架: XXL-Job / ElasticJob, 自动分片 + 故障转移
 *
 *   当前微服务只起 1 个 order 实例, 暂时不会重复. 笔记里有详细对比.
 */
@Component
public class LogisticsScheduledTask {

    private static final Logger log = LoggerFactory.getLogger(LogisticsScheduledTask.class);

    /** 自动签收的等待天数 (生产常见值: 7 / 15) */
    private static final int AUTO_SIGN_DAYS = 7;

    @Autowired
    private OrdersMapper ordersMapper;

    /**
     * 每小时整点扫一次, 把 SHIPPED 且发货 >= 7 天的订单改 COMPLETED
     *
     * cron 6 字段: 秒 分 时 日 月 周
     *   "0 0 * * * ?"   = 每小时整点 (0 秒 0 分 任意时)
     *   "0 0/30 * * * ?" 每 30 分钟 (调试时可改这个看效果)
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void autoSignTimeoutOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(AUTO_SIGN_DAYS);
        log.info("[定时任务] 自动签收扫描开始, cutoff={}", cutoff);

        // 查所有 SHIPPED + shipTime <= cutoff 的订单
        // 注意: 用 LambdaQueryWrapper 写法和直接 SQL 等价, 但更类型安全
        //   eq      → =
        //   le      → <=
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<Orders>()
                .eq(Orders::getStatus, OrderStatus.SHIPPED)
                .le(Orders::getShipTime, cutoff);

        List<Orders> overdueOrders = ordersMapper.selectList(wrapper);

        if (overdueOrders.isEmpty()) {
            log.info("[定时任务] 无超时订单, 跳过");
            return;
        }

        // 逐个改状态 (订单量大的话应该改成批量 UPDATE, 这里教学版用循环更清晰)
        int success = 0;
        for (Orders order : overdueOrders) {
            try {
                order.setStatus(OrderStatus.COMPLETED);
                order.setFinishTime(LocalDateTime.now());
                ordersMapper.updateById(order);
                success++;
                log.info("[定时任务] 自动签收 orderId={} orderNo={}", order.getId(), order.getOrderNo());
            } catch (Exception e) {
                // ⭐ 关键: 单个失败不能中断整体, 否则一条脏数据卡死所有
                log.error("[定时任务] 自动签收失败 orderId={}", order.getId(), e);
            }
        }

        log.info("[定时任务] 自动签收完成, 总计 {} 单, 成功 {} 单", overdueOrders.size(), success);
    }
}
