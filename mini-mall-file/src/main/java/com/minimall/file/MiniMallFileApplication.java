package com.minimall.file;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * mini-mall-file 对象存储服务启动类
 *
 * 跟其它服务的差别:
 *   - 没有 @MapperScan       (不连 DB)
 *   - 没有 @EnableFeignClients (暂不调其它服务)
 *
 * @ComponentScan("com.minimall") 扫到 com.minimall 根包:
 *   原因: common-core / common-security 里的 Bean 包名是 com.minimall.common.*
 *         不扩根包会缺 Bean (Result/全局异常/SecurityContextHolder 全引不到)
 */
@SpringBootApplication
@ComponentScan("com.minimall")
public class MiniMallFileApplication {

    public static void main(String[] args) {
        SpringApplication.run(MiniMallFileApplication.class, args);
    }
}
