package com.minimall.search.controller;


import com.minimall.common.core.domain.Result;
import com.minimall.search.dto.ProductSearchRequest;
import com.minimall.search.service.IProductSearchService;
import com.minimall.search.vo.PageResultVO;
import com.minimall.search.vo.ProductSearchVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
/**
 * 商品搜索 Controller (search 服务对外 HTTP 入口)
 * <p>
 * 4 个端点:
 *   POST   /search/sync                — 全量同步 (运维触发, 首次部署/数据修复)
 *   POST   /search/sync/{productId}    — 单条同步 (后续 MQ 通知触发)
 *   DELETE /search/{productId}         — 单条删除 (商品下架时调用)
 *   GET    /search/product             — 搜索 (前端核心入口)
 */
@RestController
@RequestMapping("/search")
@Slf4j
public class ProductSearchController {
    @Resource
private IProductSearchService searchService;
    // 1. 全量同步
    @PostMapping("/sync")
    public Result<Integer> syncAll() {
        // 调 service.syncAll(), 返同步数量
        int count = searchService.syncAll();
        return Result.success(count);
    }

    // 2. 单条同步
    @PostMapping("/sync/{productId}")
    public Result<Void> syncById(@PathVariable Long productId) {
        // 调 service.syncById(productId), 然后 Result.success()
        searchService.syncById(productId);
        return Result.success();
    }

    // 3. 单条删除
    @DeleteMapping("/{productId}")
    public Result<Void> deleteById(@PathVariable Long productId) {
        // 调 service.deleteById(productId), 然后 Result.success()
        searchService.deleteById(productId);
        return Result.success();
    }

    // 4. 搜索 — 核心入口
    @GetMapping("/product")
    public Result<PageResultVO<ProductSearchVO>> search(ProductSearchRequest request) {
        // 调 service.search(request)
        PageResultVO<ProductSearchVO>result=searchService.search(request);
        return Result.success(result);
    }


}
