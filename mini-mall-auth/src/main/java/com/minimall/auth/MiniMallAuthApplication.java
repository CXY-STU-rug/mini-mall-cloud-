package com.minimall.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

/**
 * mini-mall-auth 认证服务启动类
 *
 * 跟 user 启动类的差别:
 *   - 没有 @MapperScan      → auth 不直接访问数据库, 所有 user 表读写走 Feign
 *   - 有 @EnableFeignClients → 扫 com.minimall.auth.client 下的 @FeignClient 接口
 *
 * @ComponentScan("com.minimall") 必须扩到 com.minimall 而不是 com.minimall.auth
 *   原因: common-core 和 common-security 里的 Bean (JwtUtil/SecurityContextHolder)
 *         包名是 com.minimall.common.*, 不扩根包就扫不到, 启动会缺 Bean
 */
@SpringBootApplication
@ComponentScan("com.minimall")
@EnableFeignClients(basePackages = "com.minimall.auth.client")
public class MiniMallAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(MiniMallAuthApplication.class, args);
        System.out.println("=========== mini-mall-auth 启动成功 ===========");
    }
}
