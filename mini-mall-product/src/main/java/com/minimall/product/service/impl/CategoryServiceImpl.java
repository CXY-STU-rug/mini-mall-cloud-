package com.minimall.product.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.minimall.product.entity.Category;
import com.minimall.product.mapper.CategoryMapper;
import com.minimall.product.service.ICategoryService;
import org.springframework.stereotype.Service;

/**
 * Category Service 实现
 *
 * 0 行业务逻辑, 全靠继承 ServiceImpl<CategoryMapper, Category> 白嫖:
 *   ServiceImpl 内部自动持有 baseMapper (= CategoryMapper)
 *   而且实现了 IService 接口里所有方法
 *
 * 后续要加【自定义业务方法】, 比如"只查启用的+按 sort 排序", 在这写就行
 */
@Service
public class CategoryServiceImpl
        extends ServiceImpl<CategoryMapper, Category>
        implements ICategoryService {
}
