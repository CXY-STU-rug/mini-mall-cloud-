package com.minimall.search.client;
import com.minimall.common.core.domain.Result;

import com.minimall.search.entity.ProductSource;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "mini-mall-product", fallback = ProductFeignClientFallback.class)
public interface ProductFeignClient {
    @GetMapping("/product/internal/all")
   Result<List<ProductSource>> listAllForSync();//跨服务查全部产品的接口方法，直接业务调用
}