package com.minimall.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimall.product.entity.Category;
import org.apache.ibatis.annotations.Mapper;

/**
 * Category Mapper
 *
 * 继承 BaseMapper<Category> 白嫖 MP 16 个通用方法:
 *   selectById / selectList / selectOne / selectCount / selectPage
 *   insert / updateById / update / deleteById / deleteBatchIds / ...
 * 完全不用写 xml, 简单查询【0 行实现代码】
 *
 * @Mapper: MyBatis 扫描标记, 把这个接口变成代理 Bean 注入 Service
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}
