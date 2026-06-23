package com.minimall.search.dto;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductSearchRequest {

    /** 关键字: 搜 name/description/detail 三个字段 */
    private String keyword;
    private Long categoryId;//产品分类字段
    private BigDecimal minPrice;//最低价格
    private BigDecimal maxPrice;//最高价格
    private Integer page;//第几页
    private Integer size;//每页大小
    private String sort;//排序模式: price_asc/price_desc/sales_desc/rating_desc/newest, 可空
}