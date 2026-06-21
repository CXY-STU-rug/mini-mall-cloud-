package com.minimall.product.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.minimall.product.entity.Product;
import com.minimall.product.mapper.ProductMapper;
import com.minimall.product.service.IProductService;
import org.springframework.stereotype.Service;

/**
 * Product Service 实现
 * extends ServiceImpl<ProductMapper, Product> 白嫖 save/getById/list/page... 通用 CRUD
 */
@Service
public class ProductServiceImpl
        extends ServiceImpl<ProductMapper, Product>
        implements IProductService {

    // 自定义业务方法在这里加
}
