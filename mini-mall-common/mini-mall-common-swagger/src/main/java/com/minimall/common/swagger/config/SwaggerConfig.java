package com.minimall.common.swagger.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 公共 Swagger / Knife4j 配置 (mini-mall-cloud 所有业务服务统一使用).
 * <p>
 * 业务服务只要 pom 引 mini-mall-common-swagger, 启动类 @ComponentScan("com.minimall")
 * 就会自动扫到这个配置, 不用各服务重复写.
 * <p>
 * 启动后访问: http://127.0.0.1:{port}/doc.html
 */
@Configuration
public class SwaggerConfig {

    /** 取当前服务的 spring.application.name 作为文档标题, 拿不到就用默认 */
    @Value("${spring.application.name:mini-mall-cloud}")
    private String appName;

    @Bean
    public OpenAPI miniMallOpenAPI() {
        // ① 文档头部信息
        Info info = new Info()
                .title(appName + " API 文档")
                .description("mini-mall-cloud 微服务电商 API (基于 Knife4j + OpenAPI 3)")
                .version("0.0.1-SNAPSHOT")
                .contact(new Contact()
                        .name("CXY-STU-rug")
                        .url("https://github.com/CXY-STU-rug/mini-mall-cloud"))
                .license(new License().name("MIT"));

        // ② 定义 JWT Bearer 鉴权方案 (面试常考: HTTP type + bearer scheme + JWT format)
        SecurityScheme jwtScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT token (登录后从 /user/login 拿)");

        // ③ 组装: info + components(里面挂鉴权方案) + 全局应用鉴权
        return new OpenAPI()
                .info(info)
                .components(new Components()
                        .addSecuritySchemes("Bearer", jwtScheme))    // 注册名字叫 "Bearer"
                .addSecurityItem(new SecurityRequirement().addList("Bearer"));
        //                                            ^^^^ 引用上面 addSecuritySchemes 的名字
    }
}
