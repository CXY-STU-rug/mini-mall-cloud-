package com.minimall.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 请求日志 Filter (D5 新增)
 * <p>
 * 职责:
 *   ① 给每个请求生成一个 traceId (8 位短 UUID, 便于人眼读)
 *   ② 把 traceId 塞进 X-Trace-Id header 透传给下游
 *   ③ 打一条入口日志 [traceId] -> METHOD path
 * <p>
 * 执行顺序:
 *   getOrder() 必须比 AuthGlobalFilter(-100) 更小, 必须最先跑
 *   否则鉴权失败的请求(401) 不会被记录, 无法排查"谁在攻击"
 * <p>
 * 为什么 traceId 不放 ThreadLocal?
 *   WebFlux 是异步的, 同一个请求会在多个 EventLoop 线程间漂移
 *   ThreadLocal 会丢, HTTP header 才能稳定透传跨进程
 * <p>
 * 跟单体里的 Interceptor 对比:
 *   单体: 一个 LogInterceptor 在 preHandle 打日志, 同一线程能拿到 MDC
 *   微服务: GlobalFilter 在请求最早期塞 header, 跨进程也能跟
 */
@Component
public class RequestLogFilter implements GlobalFilter, Ordered {

    // 手动拿 Logger (gateway 模块没引 lombok)
    // 引入 lombok 后可改成类上加 @Slf4j 注解
    private static final Logger log = LoggerFactory.getLogger(RequestLogFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        // ─── 拿到 request ───────────────────────────────────
        ServerHttpRequest request = exchange.getRequest();

        // ─── ⭐ TODO ①: 生成 traceId (8 位短串) ──────────────
        //   提示: UUID.randomUUID().toString().substring(0, 8)
        //   为什么 8 位? 全 36 位太长肉眼不友好, 8 位足够本机内区分
        String traceId = UUID.randomUUID().toString().substring(0, 8); // [你来填这一行]

        // ─── ⭐ TODO ②: 用 mutate 加 X-Trace-Id header ────────
        //   提示: 跟 AuthGlobalFilter 第 ⑤ 步一模一样的套路
        //   写法: request.mutate().header("X-Trace-Id", traceId).build()
        ServerHttpRequest mutated = request.mutate().header("X-Trace-Id", traceId).build(); // [你来填这一行]

        // ─── ⭐ TODO ③: 打入口日志 ─────────────────────────
        //   提示: log.info("[{}] -> {} {}", traceId, 方法, path);
        //   方法: request.getMethod()
        //   path: request.getURI().getPath()
        // [你来填这一行]
        log.info("[{}] -> {} {}", traceId,request.getMethod() ,request.getURI().getPath());
        // ─── 把改过 header 的 request 塞回, 继续走过滤器链 ───
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    /**
     * ⭐ TODO ④: 返回执行顺序数字
     *   要求: 必须比 AuthGlobalFilter 的 -100 更小
     *   推荐: -150
     */
    @Override
    public int getOrder() {
        return -150; // [改成 -150]
    }
}
