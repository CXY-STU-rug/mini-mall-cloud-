package com.minimall.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimall.common.core.domain.Result;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;

import jakarta.annotation.PostConstruct;

/**
 * Sentinel Gateway 自定义降级响应 (F2.7)
 * <p>
 * 重要认知:
 *   SCA 2023.0.1.2 的 SentinelSCGAutoConfiguration 已经【自动注册】了:
 *     - SentinelGatewayFilter          (核心拦截器)
 *     - SentinelGatewayBlockExceptionHandler  (默认异常处理)
 *   所以这里【不再手动 @Bean】, 避免 Bean 名冲突
 * <p>
 * 本类只干一件事:
 *   通过 @PostConstruct 注册【自定义 BlockRequestHandler】
 *   替换默认的"Blocked by Sentinel: FlowException" 字符串输出, 改成 JSON
 */
@Configuration
public class SentinelGatewayConfig {

    /**
     * @PostConstruct: Spring 把 Bean 创建好后, 自动调这个方法
     * (注: WebFluxCallbackManager 是全局静态注册, 无需依赖任何 Bean)
     */
    @PostConstruct
    public void initBlockHandlers() {
        // ObjectMapper 用来把 Result 对象序列化成 JSON 字符串
        ObjectMapper mapper = new ObjectMapper();

        // 自定义 handler: 接到 BlockException 时返回什么响应
        BlockRequestHandler blockRequestHandler = (exchange, throwable) -> {
            Result<Void> result = Result.error(429,
                    "网关限流: 请求太频繁, 请稍后再试 (" + throwable.getClass().getSimpleName() + ")");
            String body;
            try {
                body = mapper.writeValueAsString(result);
            } catch (JsonProcessingException e) {
                body = "{\"code\":429,\"message\":\"too many requests\"}";
            }
            // 返 ServerResponse: 429 状态码 + JSON body
            return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body);
        };

        // 注册到 Gateway 专用回调管理器
        // ⚠️ 注意是 GatewayCallbackManager 不是 WebFluxCallbackManager
        //    前者是 Spring Cloud Gateway 用的(基于 RouteId 限流)
        //    后者是普通 WebFlux 用的(基于 @SentinelResource)
        GatewayCallbackManager.setBlockHandler(blockRequestHandler);
    }
}
