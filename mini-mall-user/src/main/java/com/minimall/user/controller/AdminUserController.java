package com.minimall.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minimall.common.core.domain.Result;
import com.minimall.common.core.exception.BusinessException;
import com.minimall.user.dto.AdminUserPageDTO;
import com.minimall.user.entity.User;
import com.minimall.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 后台用户管理 Controller (ADMIN.3)
 *
 * 全部走网关 /admin/** 路径 → 网关 AuthGlobalFilter 已经做了:
 *   ① 必须带 JWT (否则 401)
 *   ② role 必须 = 1 (否则 403)
 * 所以这里不用再校验权限, 信任网关传来的 X-User-Id.
 *
 * 端点:
 *   GET /admin/user/page         分页 + 条件查询
 *   PUT /admin/user/{id}/status  启用/禁用账号
 *   PUT /admin/user/{id}/password 改密 (BCrypt 加密)
 */
@RestController
@RequestMapping("/admin/user")
public class AdminUserController {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    @Autowired
    private UserMapper userMapper;

    // ─── ① 分页 + 条件查询 ─────────────────────────
    @GetMapping("/page")
    public Result<IPage<User>> page(AdminUserPageDTO query) {
        Page<User> p = Page.of(query.getPage(), query.getSize());

        QueryWrapper<User> wrapper = new QueryWrapper<>();
        // 关键词模糊: username 或 nickname 命中即可
        if (query.getKeyword() != null && !query.getKeyword().isEmpty()) {
            wrapper.and(w -> w.like("username", query.getKeyword())
                              .or().like("nickname", query.getKeyword()));
        }
        if (query.getStatus() != null) {
            wrapper.eq("status", query.getStatus());
        }
        if (query.getRole() != null) {
            wrapper.eq("role", query.getRole());
        }
        wrapper.orderByDesc("id");

        IPage<User> result = userMapper.selectPage(p, wrapper);

        // ⭐ 兜底: 列表里清掉所有 password 密文 (entity.User 去掉 @JsonIgnore 后必须手动)
        result.getRecords().forEach(u -> u.setPassword(null));

        return Result.success(result);
    }

    // ─── ② 启用 / 禁用 ─────────────────────────────
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id,
                                      @RequestBody Map<String, Integer> body) {
        Integer status = body.get("status");
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("status 只能是 0 (禁用) 或 1 (启用)");
        }
        User u = userMapper.selectById(id);
        if (u == null) {
            throw new BusinessException("用户不存在");
        }
        u.setStatus(status.byteValue());
        u.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(u);
        return Result.success();
    }

    // ─── ③ 重置密码 ───────────────────────────────
    @PutMapping("/{id}/password")
    public Result<Void> resetPassword(@PathVariable Long id,
                                       @RequestBody Map<String, String> body) {
        String password = body.get("password");
        if (password == null || password.length() < 6) {
            throw new BusinessException("密码长度至少 6 位");
        }
        User u = userMapper.selectById(id);
        if (u == null) {
            throw new BusinessException("用户不存在");
        }
        u.setPassword(ENCODER.encode(password));
        u.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(u);
        return Result.success();
    }

    // ─── ④ ADMIN.6 看板用: 简单计数 ────────────
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        // 总用户数: null wrapper 走全表 count
        long total = userMapper.selectCount(null);

        // 今天新注册的用户数: createTime >= 今天 00:00
        QueryWrapper<User> today = new QueryWrapper<User>()
                .ge("create_time", LocalDateTime.now().toLocalDate().atStartOfDay());
        long todayNew = userMapper.selectCount(today);

        // OAuth 注册占比 (oauth_provider 非空)
        QueryWrapper<User> oauth = new QueryWrapper<User>().isNotNull("oauth_provider");
        long oauthUsers = userMapper.selectCount(oauth);

        return Result.success(Map.of(
                "totalUsers",    total,
                "todayNewUsers", todayNew,
                "oauthUsers",    oauthUsers
        ));
    }
}
