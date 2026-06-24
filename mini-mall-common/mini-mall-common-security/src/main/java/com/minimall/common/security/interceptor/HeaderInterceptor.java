package com.minimall.common.security.interceptor;

import com.minimall.common.core.context.SecurityContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 透传 Header 拦截器
 *
 * 流程:
 *   ① preHandle      : 读 X-User-Id header → 塞 SecurityContextHolder
 *   ② Controller 跑  : 业务代码 SecurityContextHolder.getUserId() 取
 *   ③ afterCompletion: 清 ThreadLocal (Tomcat 线程复用必须清)
 *
 * 这个拦截器不做"鉴权拒绝", 鉴权是网关 AuthGlobalFilter 的事.
 * 这里只负责"把 header 上的身份信息塞进当前线程上下文".
 */
@Slf4j
public class HeaderInterceptor implements HandlerInterceptor {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_NAME = "X-User-Name";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // TODO ①: 实现 3 行
        //   a. 读 request.getHeader(HEADER_USER_ID)
        String userId=request.getHeader(HEADER_USER_ID);

        //   b. 用 StringUtils.hasText(...) 判非空, 然后 SecurityContextHolder.setUserId(...)
       if (StringUtils.hasText(userId)) {
           SecurityContextHolder.setUserId(userId);
           //   c. 同样套路读 X-User-Name 塞 setUserName (可选)
           // [你的代码]
       }
        String userName=request.getHeader(HEADER_USER_NAME);
       if (StringUtils.hasText(userName)) {
           SecurityContextHolder.setUserName(userName);
       }
       return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception
            ex) {
        // TODO ②: 一行
        //   调 SecurityContextHolder.remove() 清当前线程上下文
        //   ⚠️ 不清的话, Tomcat 线程池复用时, 下一个请求会看到上一个的 userId!
        // [你的代码]
        SecurityContextHolder.remove();
    }
}