package com.minimall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.minimall.order.entity.CartItem;
import com.minimall.order.vo.CartItemVO;

import java.util.List;

/**
 * CartItem 服务接口
 *
 * 继承 IService<CartItem> 拿 25 个 MP 通用方法
 * 再加 2 个自定义业务方法:
 *   - addToCart  加购 (已存在累加, 不存在新增)
 *   - listMyCart 我的购物车 (带商品详情, 实时调 product 服务)
 */
public interface ICartItemService extends IService<CartItem> {

    /**
     * 加入购物车
     * @param userId    用户 ID (Controller 从 X-User-Id 拿)
     * @param productId 商品 ID
     * @param quantity  数量 (>0)
     */
    void addToCart(Long userId, Long productId, Integer quantity);

    /**
     * 查我的购物车 (返回 VO 列表, 含商品详情)
     * @param userId 用户 ID
     */
    List<CartItemVO> listMyCart(Long userId);
}
