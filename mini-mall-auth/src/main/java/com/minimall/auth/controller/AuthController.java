package com.minimall.auth.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.minimall.auth.client.UserFeignClient;
import com.minimall.auth.dto.AuthResponse;
import com.minimall.auth.dto.UserLoginDTO;
import com.minimall.auth.dto.UserRegisterDTO;
import com.minimall.auth.model.User;
import com.minimall.common.core.domain.Result;
import com.minimall.common.core.exception.BusinessException;
import com.minimall.common.security.util.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 本地账号认证 Controller (从 user 服务 UserController 抽出来)
 *
 * 2 个端点:
 *   POST /auth/login    本地账号登录, 返 AuthResponse(token, user)
 *   POST /auth/register 注册新账号, 直接登录返 AuthResponse
 *
 * 与原 UserController 的差别:
 *   - 路径前缀 /user/login → /auth/login
 *   - 不直接调 UserMapper, 改 Feign 调 user 服务 internal 接口
 *   - 返 String token → 返 AuthResponse(token, user) 跟 OAuth 接口统一
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    /** BCrypt 加密器, 无状态线程安全, 全类共享一份 */
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private JwtUtil jwtUtil;

    // ═══════════════════════════════════════════════════════════
    // ① 本地登录
    // ═══════════════════════════════════════════════════════════

    /**
     * 本地登录
     *
     * 流程:
     *   ① Feign 调 user 服务 byUsername 查 User (含 BCrypt 密文)
     *   ② BCrypt.matches(明文, 密文) 比对
     *   ③ jwtUtil 签 token
     *   ④ 返 AuthResponse(token, 清掉 password 的 user)
     *
     * Sentinel 限流配置跟原 UserController.login 一样 (走 user 服务时是 loginResource).
     */
    @PostMapping("/login")
    @SentinelResource(
            value = "authLoginResource",
            blockHandler = "loginBlock",
            fallback = "loginFallback"
    )
    public Result<AuthResponse> login(@Valid @RequestBody UserLoginDTO dto) {
        // ① Feign 查用户
        Result<User> resp = userFeignClient.getByUsername(dto.getUsername());
        if (resp.getCode() != 200) {
            // user 服务挂了 (Fallback 返 503)
            throw new BusinessException(resp.getMessage());
        }
        User user = resp.getData();

        // ② 没查到 / 密码不对 - 防爆破不区分 "用户不存在" vs "密码错"
        if (user == null || !ENCODER.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        // ③ 签 mini-mall 自家 JWT (ADMIN 阶段: role 一起塞)
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        // ④ 兜底清掉密文, 防止返前端
        user.setPassword(null);
        return Result.success(new AuthResponse(token, user));
    }

    /** Sentinel 限流降级 */
    public Result<AuthResponse> loginBlock(UserLoginDTO dto, BlockException ex) {
        return Result.error(429, "登录请求太频繁, 请稍后再试 (触发规则: "
                + ex.getClass().getSimpleName() + ")");
    }

    /** Sentinel 业务异常降级 (透传给 GlobalExceptionHandler 处理) */
    public Result<AuthResponse> loginFallback(UserLoginDTO dto, Throwable ex) {
        if (ex instanceof RuntimeException re) throw re;
        throw new RuntimeException(ex);
    }

    // ═══════════════════════════════════════════════════════════
    // ② 本地注册
    // ═══════════════════════════════════════════════════════════

    /**
     * 注册
     *
     * 流程:
     *   ① Feign 调 byUsername 查重 (查到说明用户名已存在 → 报错)
     *   ② BCrypt 加密密码
     *   ③ 组装 User, Feign 调 createUser 入库
     *   ④ 签 token, 注册即登录, 返 AuthResponse
     */
    @PostMapping("/register")
    public Result<AuthResponse> register(@Valid @RequestBody UserRegisterDTO dto) {
        // ① 查重
        Result<User> existsResp = userFeignClient.getByUsername(dto.getUsername());
        if (existsResp.getCode() != 200) {
            throw new BusinessException(existsResp.getMessage());
        }
        if (existsResp.getData() != null) {
            throw new BusinessException("用户名已存在");
        }

        // ② 加密
        String encryptedPassword = ENCODER.encode(dto.getPassword());

        // ③ 组装并入库 (createTime/updateTime 让 user 服务 internal Controller 兜底, 这里也设一下保险)
        User newUser = new User();
        newUser.setUsername(dto.getUsername());
        newUser.setPassword(encryptedPassword);
        // C 端 WEB.2 注册表单可选字段; null 直接传给 user 服务, DB 字段允许 null
        newUser.setPhone(dto.getPhone());
        newUser.setNickname(dto.getNickname());
        newUser.setRole((byte) 0);
        newUser.setStatus((byte) 1);
        newUser.setCreateTime(LocalDateTime.now());
        newUser.setUpdateTime(LocalDateTime.now());

        Result<User> createResp = userFeignClient.createUser(newUser);
        if (createResp.getCode() != 200 || createResp.getData() == null) {
            throw new BusinessException("注册失败: " + createResp.getMessage());
        }
        User savedUser = createResp.getData();   // 含 user 服务回填的 id

        // ④ 签 token, 注册即登录 (新注册用户 role=0)
        String token = jwtUtil.generateToken(savedUser.getId(), savedUser.getUsername(), savedUser.getRole());
        savedUser.setPassword(null);             // 兜底
        return Result.success(new AuthResponse(token, savedUser));
    }
}
