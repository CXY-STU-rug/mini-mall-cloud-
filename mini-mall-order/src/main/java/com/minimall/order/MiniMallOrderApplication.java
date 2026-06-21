package com.minimall.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

/**
 * mini-mall-order 订单服务启动类
 *
 * G3.4 改动:
 *   - @MapperScan: 扫 mapper 接口 (CartItem 接 MP 必备)
 *   - @EnableFeignClients: 启用 Feign 客户端 (调 product 服务)
 *
 * 4 个注解干 4 件事:
 *   @SpringBootApplication = @Configuration + @EnableAutoConfiguration + @ComponentScan
 *   @ComponentScan("com.minimall") = 扫到 common-core 的全局异常等
 *   @MapperScan(...)               = 给 mapper 接口生成代理 Bean
 *   @EnableFeignClients(...)       = 给 @FeignClient 接口生成代理 Bean
 */
@SpringBootApplication
@ComponentScan("com.minimall")
@MapperScan("com.minimall.order.mapper")
@EnableFeignClients(basePackages = "com.minimall.order.client")
public class MiniMallOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(MiniMallOrderApplication.class, args);
        System.out.println("=========== mini-mall-order 启动成功 ===========");
    }
}
