package com.minimall.product.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.minimall.common.core.domain.Result;
import com.minimall.common.core.exception.BusinessException;
import com.minimall.product.entity.Product;
import com.minimall.product.service.IProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 商品控制器
 *
 * 2 个接口：
 *   GET /product/{id}   → 按 id 查商品（会被 Feign 调用）
 *   GET /product        → 查商品列表（前 10 条）
 */
@RestController
@RequestMapping("/product")
public class ProductController {

    @Autowired
    private IProductService productService;

    /** 按 id 查商品 */
    @GetMapping("/{id}")
    public Result<Product> getById(@PathVariable("id") Long id) {
        Product product = productService.getById(id);   // 白嫖自 ServiceImpl
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        return Result.success(product);
    }

    /** 查商品列表（前 10 条） */
    @GetMapping
    public Result<List<Product>> list() {
        List<Product> products = productService.list();
        return Result.success(products.subList(0, Math.min(10, products.size())));
    }

    // ════════════════════════════════════════════════════════════════
    // F2.6 演示接口: 异常比例熔断
    // ════════════════════════════════════════════════════════════════

    /**
     * 不稳定接口 (30% 概率抛业务异常)
     *
     * 用来演示 Sentinel 熔断:
     *   ① 没规则时: 30% 请求失败, 70% 成功
     *   ② 配 DegradeRule(异常率 50%, minRequestAmount 5, timeWindow 10s):
     *       a) 1 秒内 < 5 个请求 → 不统计, 不熔断
     *       b) 1 秒内 ≥ 5 个请求, 异常率 ≥ 50% → 熔断 10 秒
     *       c) 熔断期间所有请求直接 DegradeException → blockHandler 兜底
     *       d) 10 秒后 HALF-OPEN, 放 1 个试探, 成功就 CLOSED, 失败回 OPEN
     */
    @GetMapping("/flaky")
    @SentinelResource(
            value = "flakyResource",
            blockHandler = "flakyBlock"
            // 不写 fallback: 业务异常正常抛, 让 Sentinel 统计为"异常"
    )
    public Result<String> flaky() {
        // 30% 抛业务异常 (会被 Sentinel 计入 exceptionQps)
        if (ThreadLocalRandom.current().nextInt(100) < 30) {
            throw new BusinessException("flaky 接口随机失败 (模拟下游不稳定)");
        }
        return Result.success("flaky ok");
    }

    /**
     * flaky 的限流/熔断降级方法
     *
     * Sentinel 拦下(熔断打开 / 限流触发)时调这里:
     *   - DegradeException: 熔断打开
     *   - FlowException: 限流触发
     *
     * 我们返一个 Result.error 友好提示, 不让前端看到 500
     */
    public Result<String> flakyBlock(BlockException ex) {
        return Result.error(503, "下游服务繁忙, 暂时降级 (触发规则: "
                + ex.getClass().getSimpleName() + ")");
    }
}
