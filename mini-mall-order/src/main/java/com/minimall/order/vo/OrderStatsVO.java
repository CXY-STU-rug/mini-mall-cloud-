package com.minimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 后台统计看板 - 订单维度聚合 (ADMIN.6)
 *
 * 一个接口返所有指标, 前端一次请求渲染整个 Dashboard.
 * 用嵌套子类 (List<StatusCount> 等) 让前端类型也好对.
 */
@Data
public class OrderStatsVO {

    /** 订单总数 (所有未逻辑删除的) */
    private long totalOrders;

    /** 总销售额 = SUM(total_amount) WHERE status=3 (已完成) */
    private BigDecimal totalGmv;

    /** 今天创建的订单数 */
    private long todayOrders;

    /** 各状态订单数分布 (饼图数据源) */
    private List<StatusCount> statusDist;

    /** 最近 7 天每日订单数 (折线图, 当天没单的会补 0) */
    private List<DailyCount> dailyOrders;

    /** 热销商品 Top 5 (按已支付订单的销量降序) */
    private List<TopProductVO> topProducts;

    // ═════════════════════════════════════════
    // 嵌套子类
    // ═════════════════════════════════════════

    @Data
    public static class StatusCount {
        private Byte status;        // 0~4
        private String label;       // 中文 "待付款" 等
        private long count;
    }

    @Data
    public static class DailyCount {
        private String date;        // yyyy-MM-dd
        private long count;
    }

    @Data
    public static class TopProductVO {
        private Long productId;
        private String productName;
        private long totalQty;      // 累计销量
        private BigDecimal totalSales; // 累计销售额
    }
}
