package com.minimall.product.dto;

import lombok.Data;

/**
 * ADMIN.4 后台分页查询入参
 *
 * 字段全部 optional (前端可任选筛选条件):
 *   - page/size: 分页 (默认 1 / 20)
 *   - keyword:   商品名模糊匹配
 *   - categoryId: 限定某个分类
 *   - status:    0 下架 / 1 上架, null 表示不过滤
 */
@Data
public class AdminProductPageDTO {

    private Integer page = 1;       // 默认第 1 页
    private Integer size = 20;      // 默认每页 20 条
    private String keyword;         // 商品名关键词
    private Long categoryId;        // 分类筛选
    private Byte status;            // 状态筛选
}
