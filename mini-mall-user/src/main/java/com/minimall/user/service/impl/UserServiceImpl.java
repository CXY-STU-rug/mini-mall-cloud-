package com.minimall.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.minimall.common.core.exception.BusinessException;
import com.minimall.user.dto.UserLoginDTO;
import com.minimall.user.dto.UserRegisterDTO;
import com.minimall.user.entity.User;
import com.minimall.user.mapper.UserMapper;
import com.minimall.user.service.IUserService;
import com.minimall.user.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * UserServiceImpl —— 继承 MP 的 ServiceImpl 自动获得通用 CRUD
 *
 * 跟单体里的实现几乎一样，关键改动：
 *   - 包名 com.minimall.minimall.* → com.minimall.user.*
 *   - import BusinessException 改成从 common-core 拿
 */
@Service
public class UserServiceImpl
        extends ServiceImpl<UserMapper, User>
        implements IUserService {

    // ─── 静态加密器（无状态、线程安全，全类共享一份） ───
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public User register(UserRegisterDTO dto) {
        String username = dto.getUsername();
        String password = dto.getPassword();

        // ① 查"用户名是否已存在"
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        boolean exists = this.baseMapper.exists(wrapper);   // baseMapper 是 ServiceImpl 自带的 UserMapper

        // ② 已存在 → 抛业务异常（会被 GlobalExceptionHandler 接住返 500）
        if (exists) {
            throw new BusinessException("用户名已存在");
        }

        // ③ 密码 BCrypt 加密（同一密码每次加密结果都不同，因为有随机 salt）
        String encryptedPassword = ENCODER.encode(password);

        // ④ 组装 User
        User user = new User();
        user.setUsername(username);
        user.setPassword(encryptedPassword);

        // ⑤ 入库（this.save 是从 ServiceImpl 白嫖来的）
        this.save(user);

        // ⑥ 返回 user，id 已自动回填
        return user;
    }

    @Override
    public String login(UserLoginDTO dto) {
        // ① 按 username 查
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", dto.getUsername());
        User user = this.baseMapper.selectOne(wrapper);

        // ② 没查到 → 报错（注意安全：不告诉前端是"用户不存在"还是"密码错"，防爆破）
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        // ③ BCrypt 验证密码：原文 + 数据库密文 → 内部解 salt 比较
        boolean matches = ENCODER.matches(dto.getPassword(), user.getPassword());
        if (!matches) {
            throw new BusinessException("用户名或密码错误");
        }

        // ④ 签发 JWT，返回字符串
        return jwtUtil.generateToken(user.getId(), user.getUsername());
    }
}
