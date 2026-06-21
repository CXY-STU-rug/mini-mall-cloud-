package com.minimall.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimall.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * User Mapper 接口（没有实现类，MyBatis 启动时动态代理生成）
 *
 * 继承 BaseMapper<User> 自动获得 17 个方法：
 *   selectById / selectList / insert / updateById / deleteById ...
 *   单表 CRUD 完全不用写 SQL
 *
 * @Mapper 注解告诉 MyBatis 扫描这个接口
 *   （或者在启动类加 @MapperScan("com.minimall.user.mapper") 也行，二选一）
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    // 复杂 SQL 在这里写（@Select 注解或对应的 XML）
    // 现阶段什么都不用加，BaseMapper 的方法够用

}
