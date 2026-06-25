package com.minimall.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.minimall.user.entity.User;
import com.minimall.user.mapper.UserMapper;
import com.minimall.user.service.IUserService;
import org.springframework.stereotype.Service;

/**
 * UserServiceImpl
 *
 * AUTH 阶段后:
 *   - login/register 业务移到 mini-mall-auth
 *   - 这里只继承 ServiceImpl, 拿 BaseMapper 通用 CRUD 即可
 *   - BCrypt / JwtUtil 都不再需要
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
}
