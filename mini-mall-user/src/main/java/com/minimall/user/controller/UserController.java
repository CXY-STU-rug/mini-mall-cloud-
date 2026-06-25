package com.minimall.user.controller;

import com.minimall.common.core.context.SecurityContextHolder;
import com.minimall.common.core.domain.Result;
import com.minimall.common.core.exception.BusinessException;
import com.minimall.user.client.ProductFeignClient;
import com.minimall.user.entity.User;
import com.minimall.user.mapper.UserMapper;
import com.minimall.user.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * User 业务 Controller (AUTH 阶段后, 不再含 login/register, 那些移到 mini-mall-auth)
 *
 * 剩下端点:
 *   GET /user/me                                        当前用户 id (从 X-User-Id header 取)
 *   GET /user/{id}                                      按 id 查
 *   GET /user/{userId}/with-product/{productId}         Feign 跨服务演示
 *
 * ⚠️ password 兜底:
 *   AUTH 阶段去掉了 entity.User.password 上的 @JsonIgnore (为了让 internal 接口能返密文给 auth).
 *   所以对外端点返 User 时必须显式 setPassword(null), 见下面 getById / with-product.
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
     * ⭐ D4 验证接口: 检验 X-User-Id 是否真的从网关透传过来
     */
    @GetMapping("/me")
    public Result<Map<String, Object>> me() {
        Long userId = SecurityContextHolder.getUserId();
        System.out.println("[user/me] 收到的 X-User-Id = " + userId);
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("source", "X-User-Id header (透传自 gateway)");
        return Result.success(data);
    }

    /** 按 id 查 */
    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable("id") Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(null);   // ⭐ 兜底: 不返密文给前端
        return Result.success(user);
    }

    /**
     * Feign 调用演示: 拼装 [用户 + 商品] 信息
     */
    @GetMapping("/{userId}/with-product/{productId}")
    public Result<Map<String, Object>> getUserWithProduct(@PathVariable Long userId,
                                                          @PathVariable Long productId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(null);   // ⭐ 兜底

        Result<Map<String, Object>> productResp = productFeignClient.getById(productId);
        if (productResp.getCode() != 200) {
            throw new BusinessException(productResp.getMessage());
        }

        Map<String, Object> data = new HashMap<>();
        data.put("user", user);
        data.put("product", productResp.getData());
        return Result.success(data);
    }
}
