package com.minimall.common.security.interceptor;

import com.minimall.common.core.context.SecurityContextHolder;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * Feign 出站请求拦截器
 *
 * 工作时机: order/product/... 服务调 Feign Client 时,
 *           Feign 在发 HTTP 之前会调本拦截器的 apply 方法
 *           我们在这里往 RequestTemplate 加 header, 就会被发到下游
 *
 * 流程跟 HeaderInterceptor 对称:
 *   HeaderInterceptor (入站) : 读 X-User-Id → SecurityContextHolder
 *   FeignAuthInterceptor (出站): SecurityContextHolder → 写 X-User-Id
 *
 * 必须注册成 Bean (SEC.6 在 AutoConfiguration 里 @Bean), 否则 Feign 不知道你存在.
 */
@Slf4j
public class FeignAuthInterceptor implements RequestInterceptor {

    /**
     * apply: Feign 准备发 HTTP 之前, 给你最后改 RequestTemplate 的机会
     *
     * RequestTemplate 是 Feign 抽象的"待发请求", 提供 .header(name, value) 等方法.
     *
     * TODO: 实现 3-4 行
     *   1. Long uid = SecurityContextHolder.getUserId();
     *   2. 如果 uid != 0L (common-core 的约定: 没登录返回 0),
     *      调 template.header(HeaderInterceptor.HEADER_USER_ID, String.valueOf(uid));
     *   3. 同样套路处理 userName (调 SecurityContextHolder.getUserName(), 用 StringUtils.hasText
     判)
     *
     * 提示:
     *   - 复用 HeaderInterceptor.HEADER_USER_ID / HEADER_USER_NAME 两个常量
     *     (这样跟入站拦截器读的是同一个 header 名)
     *   - RequestTemplate.header(name, value) 加 header, 不返回 this (不是链式)
     */
    @Override
    public void apply(RequestTemplate template) {
        Long uid = SecurityContextHolder.getUserId();
        String uname = SecurityContextHolder.getUserName();
        if (uid != 0L) {
            template.header(HeaderInterceptor.HEADER_USER_ID, String.valueOf(uid));
        }
        if (org.springframework.util.StringUtils.hasText(uname)) {
            template.header(HeaderInterceptor.HEADER_USER_NAME, uname);
        }

    }
}