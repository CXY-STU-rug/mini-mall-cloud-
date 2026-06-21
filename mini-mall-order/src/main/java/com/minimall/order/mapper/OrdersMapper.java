package com.minimall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimall.order.entity.Orders;

/**
 * 订单主表 Mapper
 *
 * 不写 @Mapper 注解, 因为启动类已经 @MapperScan("com.minimall.order.mapper") 扫描了
 * (G3.4 加 CartItemMapper 时配的, 此处复用)
 *
 * 继承 BaseMapper<Orders>, 自动获得:
 *   selectById / selectList / selectPage / insert / updateById / deleteById ...
 *
 * 注: XML 文件不搬 (单体的 OrdersMapper.xml 是空壳, 没自定义 SQL)
 *     需要复杂 SQL 时再加 src/main/resources/mapper/OrdersMapper.xml
 */
public interface OrdersMapper extends BaseMapper<Orders> {
}
