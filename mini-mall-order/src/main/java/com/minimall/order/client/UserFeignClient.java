package com.minimall.order.client;

import com.minimall.common.core.domain.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

/**
 * User 服务的 Feign 客户端 (order 服务用, 给 createOrder 拿收货地址)
 *
 * ════════════════════════════════════════════════════════════════
 * 跟 ProductFeignClient 同样套路, 这是 order 服务的第 2 个 Feign Client.
 * (feedback_concrete_first: N=2, 还不抽公共 - 等 N=3 再抽 mini-mall-user-api)
 *
 * 关键点 ⭐⭐⭐:
 *   AddressController.detail() 强制要 @RequestHeader("X-User-Id"),
 *   做越权校验 "这个地址是不是你的".
 *
 *   order 调 user 时, Feign【默认不传递 HTTP header】, 所以这里【显式声明】
 *   一个 @RequestHeader 参数, 调用方必须传 userId 进来.
 *
 *   生产环境更优雅做法: 写个全局 RequestInterceptor 自动转发所有 header,
 *   现在用显式参数, 简单直接, 等出现 3+ 个 Feign Client 都需要传同样 header 再抽.
 * ════════════════════════════════════════════════════════════════
 */
@FeignClient(
        name = "mini-mall-user",
        fallback = com.minimall.order.client.fallback.UserFeignClientFallback.class
)
public interface UserFeignClient {

    /**
     * 调 user 服务的 GET /user/address/{id}
     *
     * @param id     地址 ID
     * @param userId 当前登录用户 ID (透传到下游做越权校验)
     * @return Result<Map<String, Object>>
     *         Map 里的字段对应 Address entity:
     *           id, userId, receiver, phone, province, city, district, detail, ...
     */
    @GetMapping("/user/address/{id}")
    Result<Map<String, Object>> getAddress(
            @PathVariable("id") Long id,
            @RequestHeader("X-User-Id") Long userId
    );
}
