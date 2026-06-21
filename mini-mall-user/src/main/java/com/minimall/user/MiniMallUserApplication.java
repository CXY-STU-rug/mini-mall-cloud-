package com.minimall.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

/**
 * mini-mall-user 用户服务启动类
 *
 * 关键注解：
 *   @SpringBootApplication
 *   @ComponentScan("com.minimall")  → 扫描扩到上层，包括 common-core
 *   @MapperScan(...)                → MyBatis 扫 Mapper 接口
 *   @EnableFeignClients             → ⭐ 新增：启用 Feign 客户端
 *      扫描 @FeignClient 标记的接口，给它们生成动态代理塞进容器
 *      之后业务代码就能 @Autowired 一个远程服务的接口当本地用
 */
@SpringBootApplication
@ComponentScan("com.minimall")
@MapperScan("com.minimall.user.mapper")
@EnableFeignClients(basePackages = "com.minimall.user.client")
public class MiniMallUserApplication {

    public static void main(String[] args) {
        // Boot 启动入口：内部启 Tomcat、扫包、装配 Bean 等
        SpringApplication.run(MiniMallUserApplication.class, args);
        System.out.println("=========== mini-mall-user 启动成功 ===========");
    }
}
