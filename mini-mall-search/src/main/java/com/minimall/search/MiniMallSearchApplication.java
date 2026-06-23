package com.minimall.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

/**
 * mini-mall-search 启动类
 * <p>
 * 职责: 商品全文搜索服务, 对外暴露 /search/** REST 接口,
 *      数据通过 Feign 从 product 服务拉取后灌入 Elasticsearch.
 */
@SpringBootApplication
// 3 合一注解 = @Configuration + @EnableAutoConfiguration + @ComponentScan
// 让 Spring Boot 自动配 Tomcat / Jackson / ES 客户端等

@EnableFeignClients
// 启用 Feign 客户端扫描, 后面要写的 ProductFeignClient 接口被扫到才会注册成代理 Bean

@ComponentScan("com.minimall")
// ⭐ 关键: 默认 @SpringBootApplication 只扫 com.minimall.search 子包,
// 但 common-core 的 GlobalExceptionHandler / Result 在 com.minimall.common.core 下,
// 默认扫不到 → 全局异常失效. 手动扩到 com.minimall 把整个公司域名都纳入.
public class MiniMallSearchApplication {

    public static void main(String[] args) {
        // 启动 Spring 容器, 初始化所有 Bean, 启 Tomcat 监听 9005, 注册到 Nacos
        SpringApplication.run(MiniMallSearchApplication.class, args);
        System.out.println("=========== mini-mall-search 已启动 :9005 ===========");
    }
}
