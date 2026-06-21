package com.minimall.product.service;

import com.minimall.product.entity.Product;

import java.util.List;

/**
 * 收藏服务接口 (从单体搬, 包名 com.minimall.minimall.service → com.minimall.product.service)
 *
 * ⚠️ 注意: Favorite 不像 Category/Address 一样【extends IService<Favorite>】,
 *          因为它【根本没有 entity】! 数据全在 Redis 里, 不沾 MySQL。
 *
 * 4 个能力:
 *   add        收藏 (Redis Set 加 productId)
 *   remove     取消收藏 (Set 删 productId)
 *   listMy     列我的收藏 (取 Set 所有成员, 调 productService 拿商品详情)
 *   isFavorited  判断是否已收藏 (Set 是否包含 productId)
 */
public interface IFavoriteService {

    /** 收藏 (已存在重复 add 不报错, Set 天然去重) */
    void add(Long userId, Long productId);

    /** 取消收藏 */
    void remove(Long userId, Long productId);

    /** 我的收藏列表 (返完整商品详情, 不只是 id) */
    List<Product> listMy(Long userId);

    /** 是否已收藏 */
    boolean isFavorited(Long userId, Long productId);
}
