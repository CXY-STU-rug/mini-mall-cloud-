package com.minimall.product.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.minimall.product.entity.Product;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Product Service 接口 (G3.9 补齐单体 5 方法)
 *
 * 单体补搬: getProductDetail / updateProduct / deleteProduct / searchProducts / getHotSearch
 */
public interface IProductService extends IService<Product> {

    /** 详情(带 Redis 缓存, 10 分钟过期) */
    Product getProductDetail(Long id);

    /** 改商品 + 删缓存 */
    boolean updateProduct(Product product);

    /** 删商品(逻辑删) + 删缓存 */
    boolean deleteProduct(Long id);

    /** 分页搜索 + 多条件筛选 + 顺手记录热搜词 */
    IPage<Product> searchProducts(Integer page, Integer size,
                                  Long categoryId, String keyword,
                                  BigDecimal minPrice, BigDecimal maxPrice);

    /** 热搜 Top N (从 Redis ZSet 取) */
    List<Map<String, Object>> getHotSearch(int topN);

    /** G3.10 扣库存(原子 SQL, 库存不足返 0) - 给 order 服务 Feign 调用 */
    int deductStock(Long productId, Integer quantity);

    /** G3.10 回库存(取消/关单时用) */
    int restoreStock(Long productId, Integer quantity);
}
