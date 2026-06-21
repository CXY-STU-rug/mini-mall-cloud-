package com.minimall.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.minimall.user.dto.UserLoginDTO;
import com.minimall.user.dto.UserRegisterDTO;
import com.minimall.user.entity.User;

/**
 * User Service 接口
 *
 * 继承 IService<User>：
 *   白嫖 save/getById/list/page/updateById/removeById 等 30+ 通用方法
 *   只需声明【业务专有方法】（register/login）
 */
public interface IUserService extends IService<User> {

    /** 注册 → 返回新创建的 User（id 已回填） */
    User register(UserRegisterDTO dto);

    /** 登录 → 返回 JWT token 字符串 */
    String login(UserLoginDTO dto);
}
