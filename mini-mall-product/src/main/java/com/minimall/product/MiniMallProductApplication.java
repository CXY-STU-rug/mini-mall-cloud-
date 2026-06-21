package com.minimall.product;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * mini-mall-product 商品服务启动类
 *
 * 跟 user-service 一样的套路：
 *   @ComponentScan("com.minimall") 把 common-core 的类扫进来
 *   @MapperScan(...) 让 MyBatis 扫 Mapper 接口
 */
@SpringBootApplication
@ComponentScan("com.minimall")
@MapperScan("com.minimall.product.mapper")
public class MiniMallProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(MiniMallProductApplication.class, args);
        System.out.println("=========== mini-mall-product 启动成功 ===========");
    }
}
