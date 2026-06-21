package com.minimall.user.controller;

import com.minimall.common.core.domain.Result;
import com.minimall.common.core.exception.BusinessException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hello 测试控制器
 *
 * 目标：用最简单的 3 个接口验证 common-core 真生效
 *   /hello       → 验证 Result 包装
 *   /hello/boom  → 验证 BusinessException + GlobalExceptionHandler 兜底
 *   /hello/bug   → 验证 NullPointerException 被 Exception 兜底 handler 抓到
 *
 * 关键注解：
 *   @RestController  = @Controller + @ResponseBody
 *      → 方法返回值自动转 JSON 写回响应（不用 ModelAndView）
 *   @RequestMapping("/hello")
 *      → 类级别路径前缀，下面方法的路径会自动加这个前缀
 *   @GetMapping("/xxx")
 *      → 等价于 @RequestMapping(method=GET, value="/xxx")
 */
@RestController
@RequestMapping("/hello")
public class HelloController {

    /**
     * ① 基础接口：返回 Result 包装的字符串
     *
     * 访问：GET http://localhost:9001/hello
     * 期望响应：
     *   { "code": 200, "message": "操作成功", "data": "hi mini-mall-user" }
     *
     * TODO 你来写（1 行）：
     *   调 Result.success("hi mini-mall-user") 并 return
     */
    @GetMapping
    public Result<String> hello() {
     return Result.success("hi mini-mall-user");
    }

    /**
     * ② 业务异常接口：故意抛 BusinessException
     *
     * 访问：GET http://localhost:9001/hello/boom
     * 期望响应（被 GlobalExceptionHandler 第 1 个 handler 抓到）：
     *   { "code": 401, "message": "你没登录别瞎点", "data": null }
     * 日志：会打印 [业务异常] 请求路径=/hello/boom, 错误码=401, 消息=你没登录别瞎点
     *
     * TODO 你来写（1 行）：
     *   throw new BusinessException(401, "你没登录别瞎点");
     *   （不用 return，throw 之后方法不会再走到 return）
     */
    @GetMapping("/boom")
    public Result<Void> boom() {
        // throw 抛异常 = 方法立刻结束，后面不能写 return（编译报 Unreachable statement）
        // GlobalExceptionHandler 会接住这个异常，自动把它翻译成 Result 返回给前端
        throw new BusinessException(401, "你没登录别瞎点");
    }

    /**
     * ③ 系统异常接口：故意写一个 NullPointerException
     *
     * 访问：GET http://localhost:9001/hello/bug
     * 期望响应（被 GlobalExceptionHandler 第 4 个【兜底】handler 抓到）：
     *   { "code": 500, "message": "系统繁忙，请稍后再试", "data": null }
     * 日志：会打印 [系统异常] 请求路径=/hello/bug 加完整堆栈
     *
     * TODO 你来写（2 行）：
     *   String s = null;
     *   return Result.success(s.length());  ← 这里会 NPE
     */
    @GetMapping("/bug")
    public Result<Integer> bug() {
        // 制造一个 NPE：s 是 null，调 .length() 直接炸
        // 这种异常没有专门的 @ExceptionHandler 接，会被【兜底的 Exception handler】抓到
        String s = null;
        return Result.success(s.length());
    }
}
