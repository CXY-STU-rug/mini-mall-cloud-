package com.minimall.user.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.minimall.common.core.domain.Result;
import com.minimall.common.core.exception.BusinessException;
import com.minimall.user.client.ProductFeignClient;
import com.minimall.user.dto.UserLoginDTO;
import com.minimall.user.dto.UserRegisterDTO;
import com.minimall.user.entity.User;
import com.minimall.user.mapper.UserMapper;
import com.minimall.user.service.IUserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * User 控制器
 *
 * 3 个接口：
 *   GET  /user/{id}      → 按 id 查（直接走 Mapper）
 *   POST /user/register  → 注册（走 Service）
 *   POST /user/login     → 登录返 JWT（走 Service）
 *
 * 学习点：
 *   ① Controller 是【接口层】，简单查询可以直接调 Mapper（getById），
 *      复杂业务必须经过 Service（register/login）
 *   ② @Valid + @RequestBody → 触发 DTO 字段校验
 *      校验失败抛 MethodArgumentNotValidException
 *      → GlobalExceptionHandler 第 53~62 行的 handler 接住返 400
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private IUserService userService;

    @Autowired
    private ProductFeignClient productFeignClient;

    /**
     * ⭐ D4 验证接口：检验 X-User-Id 是否真的从网关透传过来了
     *
     * 端到端链路：
     *   ① 前端带 Authorization: Bearer xxx 请求 http://localhost:9080/user/me
     *   ② 网关 AuthGlobalFilter 解析 token，把 userId 塞进 X-User-Id header
     *   ③ 网关转发到 http://localhost:9001/user/me
     *   ④ 这里收到 X-User-Id —— 证明跨进程身份透传成功
     *
     * 微服务里这是【ThreadLocal 在单体里】的等价方案：
     *   单体：Interceptor 解析 token → 放 BaseContext (ThreadLocal) → Controller 取
     *   微服务：Gateway 解析 token → 放 X-User-Id header → 下游 Controller 取
     */
    @GetMapping("/me")
    public Result<Map<String, Object>> me(
            // @RequestHeader("X-User-Id") —— 直接把 header 注入成方法参数
            // Spring 自动把 String 转成 Long（因为我们声明类型是 Long）
            @RequestHeader("X-User-Id") Long userId
    ) {
        // 打印到控制台，方便从启动窗口直接看到验证结果
        System.out.println("[user/me] 收到的 X-User-Id = " + userId);

        // 返回里同时带 userId 和 source，方便调用端确认数据来源
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("source", "X-User-Id header (透传自 gateway)");
        return Result.success(data);
    }

    /** ① 按 id 查 */
    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable("id") Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return Result.success(user);
    }

    /**
     * ② 注册
     * POST /user/register
     * Body: { "username": "bob", "password": "123456" }
     */
    @PostMapping("/register")
    public Result<User> register(@Valid @RequestBody UserRegisterDTO dto) {
        return Result.success(userService.register(dto));
    }

    /**
     * ③ 登录返 JWT
     * POST /user/login
     * Body: { "username": "alice", "password": "123456" }
     * 返回 data 字段直接是 token 字符串（前端要 localStorage 保存它）
     */
    /**
     * ③ 登录返 JWT (F2 加 Sentinel 限流)
     *
     * @SentinelResource 三个参数:
     *   value = "loginResource"
     *     → 资源名, Dashboard 上配规则时按这个名字
     *
     *   blockHandler = "loginBlock"
     *     → 被【限流/熔断/系统保护】拦截时调这个方法
     *     → 同类里, 方法签名必须跟原方法【参数+返回值】完全一致, 末尾加 BlockException
     *
     *   fallback = "loginFallback"
     *     → 业务【抛 RuntimeException】时调这个 (跟 blockHandler 互补)
     *     → 注意: BusinessException 也会触发 fallback
     */
    @PostMapping("/login")
    @SentinelResource(
            value = "loginResource",
            blockHandler = "loginBlock",
            fallback = "loginFallback"
    )
    public Result<String> login(@Valid @RequestBody UserLoginDTO dto) {
        return Result.success(userService.login(dto));
    }

    /**
     * Sentinel 限流降级方法
     *
     * 必须满足:
     *   ① public 方法
     *   ② 跟原方法【同一个类】(或 blockHandlerClass 指定外部类)
     *   ③ 返回值类型 + 参数列表跟原方法一致, 末尾追加 BlockException
     *
     * Sentinel 拦下请求后会调这里, 我们返一个友好提示, 不让前端看到 500
     */
    public Result<String> loginBlock(UserLoginDTO dto, BlockException ex) {
        // ex.getClass().getSimpleName() 可以拿到具体哪个规则触发
        // FlowException = 限流  DegradeException = 熔断  SystemBlockException = 系统保护
        return Result.error(429, "登录请求太频繁, 请 1 秒后再试 (触发规则: "
                + ex.getClass().getSimpleName() + ")");
    }

    /**
     * Sentinel 业务异常降级方法
     *
     * 触发场景: login 方法内部抛 RuntimeException (例如 BusinessException "密码错")
     * 注意: 如果不写 fallback, 这种异常会直接被 GlobalExceptionHandler 接住
     *
     * 这里我们仍然让原异常透传走, 让 GlobalExceptionHandler 处理 (跟原逻辑保持一致)
     * → 所以 fallback 实际上是把异常重新抛出
     */
    public Result<String> loginFallback(UserLoginDTO dto, Throwable ex) {
        // 把原异常透传出去, 让 GlobalExceptionHandler 处理
        if (ex instanceof RuntimeException re) throw re;
        throw new RuntimeException(ex);
    }

    /**
     * ④ Feign 调用演示：拼装【用户 + 商品】信息
     *
     * 访问：GET http://localhost:9001/user/{userId}/with-product/{productId}
     *
     * 内部流程：
     *   ① user-service 自己查 user 表
     *   ② user-service 用 Feign 调用 product-service 的 /product/{id}（HTTP 调用）
     *   ③ 拼装两个数据返给前端
     *
     * 这是【跨服务调用】最经典的场景演示。
     */
    @GetMapping("/{userId}/with-product/{productId}")
    public Result<Map<String, Object>> getUserWithProduct(@PathVariable Long userId,
                                                          @PathVariable Long productId) {
        // ① 查本地用户
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // ② ⭐ Feign 调远程服务，看起来像调本地接口，实际是一次 HTTP 调用
        Result<Map<String, Object>> productResp = productFeignClient.getById(productId);
        if (productResp.getCode() != 200) {
            // product 服务返回了业务错误（比如商品不存在）
            throw new BusinessException(productResp.getMessage());
        }

        // ③ 拼装返回
        Map<String, Object> data = new HashMap<>();
        data.put("user", user);
        data.put("product", productResp.getData());
        return Result.success(data);
    }
}
