package com.minimall.gateway;

import com.minimall.common.core.exception.GlobalExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * mini-mall-gateway 网关启动类
 *
 * F2.7 修订: 排除 common-core 的 GlobalExceptionHandler
 *   原因: 它的 @ExceptionHandler(Exception.class) 兜底会吞掉 Sentinel 的 BlockException,
 *         导致网关限流返 500 而不是 429
 *   方案: WebFlux 的全局异常用 SentinelGatewayBlockExceptionHandler + WebExceptionHandler
 *         不需要 common-core 那个 MVC 风格的全局异常
 */
@SpringBootApplication
@ComponentScan(
        value = "com.minimall",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = GlobalExceptionHandler.class
        )
)
public class MiniMallGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(MiniMallGatewayApplication.class, args);
        System.out.println("=========== mini-mall-gateway 启动成功 ===========");
    }
}
