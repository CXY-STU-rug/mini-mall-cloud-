package com.minimall.user.client;

import com.minimall.common.core.domain.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * Product 服务的 Feign 客户端
 *
 * 重点理解：
 *   ① @FeignClient 标记的接口【不需要实现类】
 *      启动时 Spring 用动态代理生成实例放进容器
 *      就跟 MyBatis Mapper 的玩法一模一样
 *
 *   ② name = "mini-mall-product"
 *      暂时只是个【标识】（用来区分多个 Feign 客户端 + 配置隔离）
 *      接 Nacos 之后会变成【服务发现的服务名】，会自动找注册中心的服务实例
 *
 *   ③ E3 之后 url 已删除
 *      Feign 看到没有 url, 就走 LoadBalancer + Nacos 服务发现:
 *        - 启动时订阅 mini-mall-product 服务的实例列表
 *        - 每次调用挑一个健康实例转发(默认轮询)
 *        - product 挂了 Nacos 自动剔除, Feign 不会再选它
 *      这是 Feign + Nacos 的标准用法, 完全无感知地切换到了服务发现
 *
 *   ④ 返回类型为啥用 Map<String, Object> 而不是 Result<Product>?
 *      因为 Product 类在 mini-mall-product 模块, user-service 引不到
 *      简单方案: 用 Map 接, 业务代码自己 get("data") 拿字段
 *      正经方案: 抽 mini-mall-product-api 模块只放 DTO 共用(以后做)
 */
@FeignClient(
        name = "mini-mall-product",
        fallback = com.minimall.user.client.fallback.ProductFeignClientFallback.class
)
public interface ProductFeignClient {

    /**
     * 调 product 服务的 GET /product/{id}
     * 方法签名要跟对方 Controller 的接口对齐：HTTP 方法、路径、参数注解
     */
    @GetMapping("/product/{id}")
    Result<Map<String, Object>> getById(@PathVariable("id") Long id);
}
