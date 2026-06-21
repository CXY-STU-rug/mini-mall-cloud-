package com.minimall.gateway.filter;

import com.minimall.gateway.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 全局鉴权过滤器
 * <p>
 * 工作原理：
 *   ① 每个进入网关的请求，都会经过 filter() 方法
 *   ② 白名单内的（登录/注册）直接放行
 *   ③ 其他请求：从 Authorization header 拿 token，解析校验
 *   ④ 成功 → 把 userId 塞进 X-User-Id header 透传给下游
 *   ⑤ 失败 → 返 401 直接拦下
 *
 * 实现接口：
 *   GlobalFilter —— Spring Cloud Gateway 的全局过滤器接口
 *   Ordered      —— 控制过滤器执行顺序
 *
 * 跟单体 JwtInterceptor 的 90% 一样，10% 关键不同：
 *   - 返 Mono<Void>（响应式），不是 boolean
 *   - 用 ServerWebExchange 取/改请求，不是 HttpServletRequest
 *   - 透传方案：HTTP header（X-User-Id），不是 ThreadLocal
 */
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 白名单：这些 path 不校验 token，直接放行
     *
     * ⭐ TODO ①：你来填白名单数组
     *   提示：根据 D3 路线图，至少要放行登录和注册
     *   写法：List.of("/user/login", "/user/register")
     */
    private static final List<String> WHITE_LIST = List.of(
            // 登录/注册
            "/user/login", "/user/register",
            // G3.1: 商品分类对游客也可见 (列表/详情)
            // 注: startsWith 会把 POST/PUT/DELETE 也放过, 教学项目暂不区分
            //     生产环境应该用 method + path 双维度白名单
            "/category"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        // 拿到当前请求对象
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // ⭐ TODO ②：判断当前 path 是否在白名单
        //   如果在 → 直接放行（return chain.filter(exchange);）
        //   提示：用 WHITE_LIST.stream().anyMatch(path::startsWith)
        //         或者用一个 for 循环判断
        // 在白名单 → 不校验，直接走下游
        // [你的代码写这里]
        boolean inWhiteList = WHITE_LIST.stream().anyMatch(path::startsWith);
        if (inWhiteList) {
            return chain.filter(exchange);
        }

        // ⭐ TODO ③：从 header 取 Authorization
        //   提示：request.getHeaders().getFirst("Authorization")
        //   token 一般以 "Bearer " 开头，要去掉这 7 个字符
        //   如果 token 为 null 或不以 Bearer 开头 → 调 unauthorized(exchange) 返 401  String token = request.getHeaders().getFirst("Authorization");
        // 去掉 "Bearer " 前缀，注意有个空格 = 7 字符

        // [你的代码：null / Bearer 校验]

        String token = request.getHeaders().getFirst("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            return unauthorized(exchange);
        }
        token = token.substring(7);   // 去掉 "Bearer " 前缀，注意有个空格 = 7 字符
        // ⭐ TODO ④：解析 token，拿 userId
        //   提示：jwtUtil.getUserIdFromToken(token)
        //   解析失败会抛 io.jsonwebtoken 的各种异常 → catch 后返 401
        // ← 改成解析的结果

        Long userId;
        try {
            userId = jwtUtil.getUserIdFromToken(token);
        } catch (Exception e) {
            return unauthorized(exchange);
        }


        // ⭐ TODO ⑤：把 userId 塞进 X-User-Id header 透传给下游
        //   提示：request.mutate().header("X-User-Id", String.valueOf(userId)).build()
        //   然后再用 exchange.mutate().request(mutated).build()
        //   最后调 chain.filter(...)  让请求继续走

        // [你的代码：return chain.filter(...)]
        ServerHttpRequest mutated= request.mutate().header("X-User-Id", String.valueOf(userId)).build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    /**
     * 返回 401 未授权（公共方法，TODO 里多处复用）
     *
     * WebFlux 风格的"中断响应"：
     *   ① 设置 401 状态码
     *   ② 调 setComplete() 直接结束响应链
     *   ③ 不调 chain.filter() 就意味着请求到此为止，不会转发到下游
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    /**
     * Filter 执行顺序：数字越小越先执行
     *
     * 为啥要 -100？
     *   Gateway 内部有很多默认 Filter（路由匹配、负载均衡等）
     *   我们的鉴权要在所有路由处理【之前】跑
     *   -100 比绝大部分默认 Filter 都小，能确保最先执行
     */
    @Override
    public int getOrder() {
        return -100;
    }
}
