package com.minimall.order.controller;

// ⭐ Result 是从 common-core 拿的, 不是 order 自己写的
// 这就是 common-core 的价值: 3 个服务返回格式统一, 改 1 处 3 处生效
import com.minimall.common.core.domain.Result;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * order 服务最小可用接口
 *
 * 作用:
 *   ① 验证 order 服务能跑起来 (端口/启动类/包扫描都对)
 *   ② 验证 common-core 真的拿到了 Result 类
 *   ③ 后面通过 Nacos 控制台看 mini-mall-order 服务注册成功了
 *
 * 怎么测:
 *   直连 (跳过网关):  curl http://127.0.0.1:9003/order/hello
 *   走网关 (要加路由): curl http://127.0.0.1:9080/order/hello   ← 网关现在没配, 暂不通
 */
@RestController                       // ① 告诉 Spring: 这是个 Controller, 方法返的对象自动序列化成 JSON
@RequestMapping("/order")             // ② 类级前缀, 类里所有方法都以 /order 开头
public class HelloController {

    /**
     * GET /order/hello
     *
     * 返回:
     *   { "code": 200, "message": "成功", "data": "hello from order" }
     *
     * Result<String> 是泛型, 意思是【data 字段是 String】
     * 前端 / curl 拿到的就是上面那个 JSON
     */
    @GetMapping("/hello")             // ③ 方法绑 GET, 最终路径 = 类 /order + 方法 /hello
    public Result<String> hello() {

        // Result.success(data) 是 common-core 写的静态工厂方法:
        //   public static <T> Result<T> success(T data) {
        //       return new Result<>(200, "成功", data);
        //   }
        // 不用 new Result<>() 拼装, 短小整洁
        return Result.success("hello from order");
    }
}
