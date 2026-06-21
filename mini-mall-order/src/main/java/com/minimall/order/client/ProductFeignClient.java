package com.minimall.order.client;

import com.minimall.common.core.domain.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * Product 服务的 Feign 客户端 (order 服务用, 给 CartItem 拿商品详情)
 *
 * 跟 user 服务里那份【几乎一模一样】, 唯一区别: 包名不同
 *   com.minimall.user.client.ProductFeignClient
 *   com.minimall.order.client.ProductFeignClient
 *
 * 又是 feedback_concrete_first 的【重复一次】, 等第 3 次 (例如 favorite 也要)
 * 再考虑抽 mini-mall-product-api 公共模块
 *
 * 重点理解:
 *
 *   ① @FeignClient 标记的接口【不需要实现类】
 *      启动时 @EnableFeignClients 触发, Spring 用 JDK 动态代理生成实例放容器
 *      跟 MyBatis Mapper 玩法一模一样: "接口 → 代理 → 注入"
 *
 *   ② name = "mini-mall-product"
 *      跟 product 的 spring.application.name 完全一致
 *      没写 url, 走【Nacos 服务发现 + LoadBalancer 负载均衡】:
 *        - 启动时订阅 mini-mall-product 服务的实例列表
 *        - 每次调用挑一个健康实例转发(默认轮询)
 *        - product 挂了 Nacos 自动剔除, Feign 不会再选它
 *
 *   ③ 返回类型 Result<Map<String, Object>> 而不是 Result<Product>:
 *      Product 类在 mini-mall-product 模块, order 模块引不到
 *      简单方案: 用 Map 接, 业务代码自己 get("name") get("price")
 *      正经方案: 抽 mini-mall-product-api 模块只放 DTO, 双方都引
 */
@FeignClient(name = "mini-mall-product")
public interface ProductFeignClient {

    /**
     * 调 product 服务的 GET /product/{id}
     *
     * 方法签名要跟对方 Controller 的接口对齐:
     *   - HTTP 方法 (@GetMapping)
     *   - 路径    (/product/{id})
     *   - 参数注解 (@PathVariable)
     *
     * Spring Cloud Feign 看这些注解, 编织出 HTTP 请求发出去
     */
    @GetMapping("/product/{id}")
    Result<Map<String, Object>> getById(@PathVariable("id") Long id);
}
