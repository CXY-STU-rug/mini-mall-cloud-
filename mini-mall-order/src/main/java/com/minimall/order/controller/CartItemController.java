package com.minimall.order.controller;

import com.minimall.common.core.context.SecurityContextHolder;
import com.minimall.common.core.domain.Result;
import com.minimall.common.core.exception.BusinessException;
import com.minimall.order.dto.AddCartDTO;
import com.minimall.order.entity.CartItem;
import com.minimall.order.service.ICartItemService;
import com.minimall.order.vo.CartItemVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 购物车 Controller (G3.4 从单体搬, SEC.10 改用 SecurityContextHolder)
 *
 * 端点 (网关代理 /cart/** → mini-mall-order):
 *   GET    /cart           → 我的购物车 (含商品详情)
 *   POST   /cart           → 加购
 *   PUT    /cart/{id}      → 改数量 (≤0 自动删除)
 *   DELETE /cart/{id}      → 删购物车项
 */
@RestController
@RequestMapping("/cart")
public class CartItemController {

    @Autowired
    private ICartItemService cartItemService;

    /**
     * ① 我的购物车
     *
     * 注意: 这里返 CartItemVO 不返 CartItem entity
     *      因为前端要的是【组合数据】(cart 自己 + 商品详情)
     */
    @GetMapping
    public Result<List<CartItemVO>> myCart() {
        Long userId= SecurityContextHolder.getUserId();
        return Result.success(cartItemService.listMyCart(userId));
    }

    /**
     * ② 加购
     *
     * 单体里没 @RequestBody, 这里加上 (前端发 JSON)
     */
    @PostMapping
    public Result<Void> add(@RequestBody AddCartDTO dto)

    {
        Long userId= SecurityContextHolder.getUserId();
        cartItemService.addToCart(userId, dto.getProductId(), dto.getQuantity());
        return Result.success();
    }

    /**
     * ③ 修改数量
     *
     * 业务规则:
     *   quantity > 0 → 更新
     *   quantity ≤ 0 → 等于删除 (单体保留的逻辑)
     *
     * 请求体: { "quantity": 3 }
     */
    @PutMapping("/{id}")
    public Result<Void> updateQuantity(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> body

    ) {
        Long userId= SecurityContextHolder.getUserId();
        // 越权校验: 这条 cart 是不是你的
        CartItem item = getAndCheckOwn(id, userId);

        Integer quantity = body.get("quantity");
        if (quantity == null || quantity <= 0) {
            cartItemService.removeById(id);   // ≤0 → 删
        } else {
            item.setQuantity(quantity);
            cartItemService.updateById(item);  // > 0 → 改
        }
        return Result.success();
    }

    /**
     * ④ 删除
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id
                             ) {
        Long userId= SecurityContextHolder.getUserId();
        getAndCheckOwn(id, userId);
        cartItemService.removeById(id);    // 逻辑删除 (因 @TableLogic)
        return Result.success();
    }

    /**
     * 私有: 查 + 越权校验 (跟 Address 那一套一样)
     */
    private CartItem getAndCheckOwn(Long id, Long currentUserId) {
        CartItem item = cartItemService.getById(id);
        if (item == null) {
            throw new BusinessException(404, "购物车项不存在");
        }
        if (!item.getUserId().equals(currentUserId)) {
            throw new BusinessException(403, "无权操作该购物车项");
        }
        return item;
    }
}
