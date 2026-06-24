package com.minimall.product.controller;

import com.minimall.common.core.context.SecurityContextHolder;
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
 * 关键改动 vs 单体 (SEC.10 重构后):
 *   userId 从 SecurityContextHolder.getUserId() 拿
 *   HeaderInterceptor 在请求进入时, 已把网关塞的 X-User-Id 头自动放进 ThreadLocal
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
    public Result<Void> add(@PathVariable Long productId) {
        Long userId= SecurityContextHolder.getUserId();
        favoriteService.add(userId, productId);
        return Result.success();
    }

    /** ② 取消收藏 */
    @DeleteMapping("/{productId}")
    public Result<Void> remove(@PathVariable Long productId
                             ) {
        Long userId= SecurityContextHolder.getUserId();
        favoriteService.remove(userId, productId);
        return Result.success();
    }

    /** ③ 我的收藏列表 */
    @GetMapping("/my")
    public Result<List<Product>> listMy() {
        Long userId= SecurityContextHolder.getUserId();
        return Result.success(favoriteService.listMy(userId));
    }

    /** ④ 是否已收藏 (用来给"心形图标"渲染状态) */
    @GetMapping("/{productId}/exists")
    public Result<Boolean> exists(@PathVariable Long productId
                                  ) {
        Long userId= SecurityContextHolder.getUserId();
        return Result.success(favoriteService.isFavorited(userId, productId));
    }
}
