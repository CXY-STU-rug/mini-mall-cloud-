package com.minimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.minimall.product.entity.Category;

/**
 * Category Service 接口
 *
 * 继承 IService<Category> 拿到 MP 封装的 25 个【通用业务方法】:
 *   getById / save / updateById / removeById / list / count / page / saveBatch / ...
 *
 * 跟 BaseMapper 的区别:
 *   BaseMapper 是【数据访问层】方法, 偏向单条 SQL
 *   IService   是【业务封装层】方法, 内部可能跑多条 SQL + 事务
 *   一般 Controller 调 Service, Service 内部偶尔调 baseMapper
 */
public interface ICategoryService extends IService<Category> {
}
