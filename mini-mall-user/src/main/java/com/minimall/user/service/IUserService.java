package com.minimall.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.minimall.user.entity.User;

/**
 * User Service 接口
 *
 * 继承 IService<User>: 白嫖 save/getById/list/page/updateById/removeById 等通用方法.
 *
 * AUTH 阶段后:
 *   - login/register 业务移到 mini-mall-auth 服务
 *   - 这里只剩 user 表的基础 CRUD (IService 已经够用, 暂时不需要专有方法)
 *   - 留着这个接口是为了将来扩展 (例如改密码、用户画像计算等)
 */
public interface IUserService extends IService<User> {
}
