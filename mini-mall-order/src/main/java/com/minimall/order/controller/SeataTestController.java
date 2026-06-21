package com.minimall.order.controller;

import com.minimall.common.core.domain.Result;
import com.minimall.order.client.ProductFeignClient;
import io.seata.core.context.RootContext;
import io.seata.spring.annotation.GlobalTransactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Seata AT 端到端验证 Controller (G5.6)
 *
 * 2 个端点直接演示 Seata 回滚:
 *
 *   POST /seata-test/deduct/{id}/{qty}                 → 扣库存, 提交
 *   POST /seata-test/deduct/{id}/{qty}?throwError=true → 扣库存, 抛异常, 回滚
 *
 * 验证逻辑:
 *   1. 看 product stock 初始值
 *   2. 调 throwError=true 端点 → product stock 一阶段先扣, 然后 TC 通知回滚
 *   3. 再看 product stock → 应等于初始值, undo_log 表的 rollback_info 被消费清空
 */
@RestController
@RequestMapping("/seata-test")
public class SeataTestController {

    private static final Logger log = LoggerFactory.getLogger(SeataTestController.class);

    @Autowired
    private ProductFeignClient productFeignClient;

    /**
     * 扣库存测试端点
     * @param throwError true=故意抛异常触发回滚, false=正常提交
     */
    @PostMapping("/deduct/{productId}/{qty}")
    @GlobalTransactional(name = "seata-test-deduct", rollbackFor = Exception.class)
    public Result<String> deduct(
            @PathVariable Long productId,
            @PathVariable Integer qty,
            @RequestParam(defaultValue = "false") boolean throwError
    ) {
        String xid = RootContext.getXID();
        log.info("[Seata-Test] 进入全局事务 XID={} productId={} qty={} throwError={}",
                xid, productId, qty, throwError);

        // 第 1 步: 通过 Feign 调 product 扣库存
        //   XID 由 Seata 自动透传到 product 的 header
        //   product 的 deductStock 内部 SQL 被代理, 写 undo_log
        Result<Integer> deductResp = productFeignClient.deductStock(productId, qty);
        if (deductResp == null || deductResp.getCode() != 200) {
            throw new RuntimeException("扣库存失败: " + (deductResp != null ? deductResp.getMessage() : "null response"));
        }

        // 第 2 步: 模拟故意抛异常 (简历级验证: 一阶段已经扣了, 二阶段才回滚)
        if (throwError) {
            log.warn("[Seata-Test] 故意抛异常 XID={}, Seata 应该回滚 product 已扣的库存", xid);
            throw new RuntimeException("Seata-Test 故意抛异常, 应该看到库存自动还原");
        }

        return Result.success("扣库存成功 XID=" + xid);
    }
}
