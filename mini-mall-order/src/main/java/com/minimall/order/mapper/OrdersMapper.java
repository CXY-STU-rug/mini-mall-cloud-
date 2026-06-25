package com.minimall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimall.order.entity.Orders;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 订单主表 Mapper
 *
 * 不写 @Mapper 注解, 因为启动类已经 @MapperScan("com.minimall.order.mapper") 扫描了
 * 继承 BaseMapper<Orders> 自动获得 CRUD 方法.
 *
 * ADMIN.6 加: 4 个 @Select 聚合方法用来出统计看板.
 *           直接写原生 SQL 比 LambdaQueryWrapper 的 select("count(*)") 可读性高,
 *           且这种聚合很难用 wrapper 简洁表达 (GROUP BY DATE() / IFNULL 等).
 */
public interface OrdersMapper extends BaseMapper<Orders> {

    /**
     * 总销售额 = 完成状态(3) 的订单金额合计
     * IFNULL: 防止一条都没时返 null, 改成返 0
     */
    @Select("SELECT IFNULL(SUM(total_amount), 0) FROM orders " +
            "WHERE is_deleted = 0 AND status = 3")
    BigDecimal sumCompletedGmv();

    /**
     * 今日新增订单数 (按 create_time)
     * CURDATE() 是 MySQL 函数, 返今天 00:00 (DATE 类型)
     */
    @Select("SELECT COUNT(*) FROM orders " +
            "WHERE is_deleted = 0 AND DATE(create_time) = CURDATE()")
    long countTodayOrders();

    /**
     * 按状态分组 count, 5 个桶
     * 注: 状态 0~4 都返, 没数据的桶 controller 层补 0
     * 返 Map: { status: 1, cnt: 3 }
     */
    @Select("SELECT status, COUNT(*) AS cnt FROM orders " +
            "WHERE is_deleted = 0 " +
            "GROUP BY status")
    List<Map<String, Object>> countByStatus();

    /**
     * 最近 7 天每日订单数
     * DATE_SUB(CURDATE(), INTERVAL 6 DAY) = 7 天前 0 点
     * 返: { day: '2026-06-19', cnt: 4 }
     * 没单的那天不会出现, controller 层补 0
     */
    @Select("SELECT DATE(create_time) AS day, COUNT(*) AS cnt FROM orders " +
            "WHERE is_deleted = 0 AND create_time >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) " +
            "GROUP BY DATE(create_time) " +
            "ORDER BY day ASC")
    List<Map<String, Object>> countDaily7d();
}
