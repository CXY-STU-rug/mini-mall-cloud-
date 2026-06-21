package com.minimall.product.controller;

import com.minimall.common.core.domain.Result;
import com.minimall.product.entity.Product;
import com.minimall.product.service.IFavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 收藏 Controller (从单体搬过来)
 *
 * 端点 (网关代理 /favorite/** → mini-mall-product):
 *   POST   /favorite/{productId}         → 收藏
 *   DELETE /favorite/{productId}         → 取消收藏
 *   GET    /favorite/my                  → 我的收藏列表 (含商品详情)
 *   GET    /favorite/{productId}/exists  → 是否已收藏
 *
 * 关键改动 vs 单体:
 *   userId 从 @RequestHeader("X-User-Id") 拿 (微服务铁律)
 *   而不是单体的 UserContext.getUserId()
 */
@RestController
@RequestMapping("/favorite")
public class FavoriteController {

    @Autowired
    private IFavoriteService favoriteService;

    /**
     * ① 收藏
     *
     * 注: 重复 add 不会报错, Redis Set 天然去重
     */
    @PostMapping("/{productId}")
    public Result<Void> add(@PathVariable Long productId,
                            @RequestHeader("X-User-Id") Long userId) {
        favoriteService.add(userId, productId);
        return Result.success();
    }

    /** ② 取消收藏 */
    @DeleteMapping("/{productId}")
    public Result<Void> remove(@PathVariable Long productId,
                               @RequestHeader("X-User-Id") Long userId) {
        favoriteService.remove(userId, productId);
        return Result.success();
    }

    /** ③ 我的收藏列表 */
    @GetMapping("/my")
    public Result<List<Product>> listMy(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(favoriteService.listMy(userId));
    }

    /** ④ 是否已收藏 (用来给"心形图标"渲染状态) */
    @GetMapping("/{productId}/exists")
    public Result<Boolean> exists(@PathVariable Long productId,
                                  @RequestHeader("X-User-Id") Long userId) {
        return Result.success(favoriteService.isFavorited(userId, productId));
    }
}
