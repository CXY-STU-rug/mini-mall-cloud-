package com.minimall.product.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minimall.common.core.domain.Result;
import com.minimall.common.core.exception.BusinessException;
import com.minimall.product.dto.AdminProductPageDTO;
import com.minimall.product.dto.AdminProductSaveDTO;
import com.minimall.product.entity.Product;
import com.minimall.product.service.IProductService;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 后台商品管理 Controller (ADMIN.4)
 *
 * 端点设计:
 *   GET    /admin/product/page              分页 + 关键词 / 分类 / 状态筛选
 *   POST   /admin/product                   新增
 *   PUT    /admin/product/{id}              编辑 (整体覆盖)
 *   PUT    /admin/product/{id}/status       仅切换上架/下架 (比整体 PUT 轻)
 *   DELETE /admin/product/{id}              逻辑删除 (@TableLogic 自动转 UPDATE is_deleted=1)
 *
 * 鉴权:
 *   网关 AuthGlobalFilter 对 /admin/** 已经校验 role=1, 这里不需要再写
 */
@RestController
@RequestMapping("/admin/product")
public class AdminProductController {

    @Autowired
    private IProductService productService;

    // ════════════════════════════════════════════════════════════
    // ① 分页查询
    // ════════════════════════════════════════════════════════════
    @GetMapping("/page")
    public Result<IPage<Product>> page(AdminProductPageDTO query) {
        // 构造分页对象
        Page<Product> p = new Page<>(query.getPage(), query.getSize());

        // 动态拼条件: 只在字段非空时加 where, MyBatis-Plus 的 LambdaQueryWrapper 支持三参版本
        LambdaQueryWrapper<Product> w = new LambdaQueryWrapper<>();
        w.like(query.getKeyword() != null && !query.getKeyword().isBlank(),
                Product::getName, query.getKeyword());
        w.eq(query.getCategoryId() != null, Product::getCategoryId, query.getCategoryId());
        w.eq(query.getStatus() != null, Product::getStatus, query.getStatus());
        w.orderByDesc(Product::getId);   // ID 倒序: 新商品在前

        IPage<Product> result = productService.page(p, w);
        return Result.success(result);
    }

    // ════════════════════════════════════════════════════════════
    // ② 新增商品
    // ════════════════════════════════════════════════════════════
    @PostMapping
    public Result<Long> create(@RequestBody @Valid AdminProductSaveDTO dto) {
        // 新建场景: 强制 id=null, 防止前端传 id 误覆盖
        dto.setId(null);

        Product p = new Product();
        BeanUtils.copyProperties(dto, p);  // DTO → Entity 字段同名拷贝
        productService.save(p);            // MP 自动填 id / createTime / updateTime
        return Result.success(p.getId());  // 返新 ID 给前端
    }

    // ════════════════════════════════════════════════════════════
    // ③ 编辑商品
    // ════════════════════════════════════════════════════════════
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @RequestBody @Valid AdminProductSaveDTO dto) {
        // 防止前端伪造 body.id != path id
        dto.setId(id);

        // 校验商品真存在 (走 @TableLogic 自动过滤已删的)
        Product existed = productService.getById(id);
        if (existed == null) throw new BusinessException("商品不存在");

        Product p = new Product();
        BeanUtils.copyProperties(dto, p);
        // 走 service 包装方法: 内部会删 Redis 详情缓存, 避免缓存不一致
        productService.updateProduct(p);
        return Result.success();
    }

    // ════════════════════════════════════════════════════════════
    // ④ 上架 / 下架
    // ════════════════════════════════════════════════════════════
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id,
                                     @RequestBody Map<String, Object> body) {
        // 前端传 {status: 0|1}, Map 这里宽容点, Integer/Byte 都接
        Object raw = body.get("status");
        if (raw == null) throw new BusinessException("status 不能为空");
        byte status = ((Number) raw).byteValue();
        if (status != 0 && status != 1) throw new BusinessException("status 只能是 0 或 1");

        Product existed = productService.getById(id);
        if (existed == null) throw new BusinessException("商品不存在");
        existed.setStatus(status);
        productService.updateProduct(existed);   // 同样走 service 删缓存
        return Result.success();
    }

    // ════════════════════════════════════════════════════════════
    // ⑤ 删除 (逻辑删)
    // ════════════════════════════════════════════════════════════
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        // service.deleteProduct() 已包: 逻辑删 + 删 Redis 缓存
        productService.deleteProduct(id);
        return Result.success();
    }

    // ════════════════════════════════════════════════════════════
    // ⑥ ADMIN.6 看板用: 简单计数
    // ════════════════════════════════════════════════════════════
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        // IService.count() 走 BaseMapper, @TableLogic 自动 WHERE is_deleted=0
        long total = productService.count();
        long onShelf = productService.count(
                new LambdaQueryWrapper<Product>().eq(Product::getStatus, (byte) 1)
        );
        return Result.success(Map.of(
                "totalProducts",  total,
                "onShelfCount",   onShelf,
                "offShelfCount",  total - onShelf
        ));
    }
}
