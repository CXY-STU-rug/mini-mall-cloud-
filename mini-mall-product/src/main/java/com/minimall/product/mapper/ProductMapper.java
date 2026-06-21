package com.minimall.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimall.product.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * Product Mapper
 *
 * 继承 BaseMapper<Product> 自动获得 17 个通用方法
 * 加 2 个自定义注解 SQL（防超卖核心 + 回滚库存）
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 扣库存（防超卖核心）
     *   - stock = stock - quantity
     *   - sales = sales + quantity
     *   - WHERE id = ? AND stock >= quantity   ← 关键：原子操作
     *     条件不满足时影响行数返回 0，Java 层据此判"库存不足"
     *
     * @return 影响行数：1=成功，0=库存不足
     */
    @Update("UPDATE product SET stock = stock - #{quantity}, sales = sales + #{quantity} " +
            "WHERE id = #{productId} AND stock >= #{quantity}")
    int deductStock(@Param("productId") Long productId,
                    @Param("quantity") Integer quantity);

    /**
     * 回滚库存（取消订单时用）
     */
    @Update("UPDATE product SET stock = stock + #{quantity}, sales = sales - #{quantity} " +
            "WHERE id = #{productId}")
    int restoreStock(@Param("productId") Long productId,
                     @Param("quantity") Integer quantity);
}
