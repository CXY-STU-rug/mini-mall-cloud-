package com.minimall.review;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

/**
 * mini-mall-review 评价服务启动类
 *
 * 4 个注解干 4 件事:
 *   @SpringBootApplication = @Configuration + @EnableAutoConfiguration + @ComponentScan("com.minimall.review")
 *   @ComponentScan("com.minimall") = 扫 common-core 的 GlobalExceptionHandler (不加扫不到, 全局异常处理失效)
 *   @MapperScan(...)               = 给 ReviewsMapper 接口生成代理 Bean
 *   @EnableFeignClients(...)       = 启用 OrdersFeignClient / ProductFeignClient
 *
 * ✗ 不加 @EnableScheduling: 评价业务没有定时任务
 */
@SpringBootApplication
@ComponentScan("com.minimall")
@MapperScan("com.minimall.review.mapper")
@EnableFeignClients(basePackages = "com.minimall.review.client")
public class MiniMallReviewApplication {

    public static void main(String[] args) {
        SpringApplication.run(MiniMallReviewApplication.class, args);
        System.out.println("=========== mini-mall-review 启动成功 ===========");
    }
}
