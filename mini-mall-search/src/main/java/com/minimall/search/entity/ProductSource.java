package com.minimall.search.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品数据载体 (Feign 调 product 服务后接收 JSON 用)
 * <p>
 * 设计原则:
 *   - 字段名跟 product 服务的 Product entity 完全一致 (Jackson 反序列化按名匹配)
 *   - 不带任何 ES/MP 注解 (它不是 ES 文档, 也不连 DB, 就是个纯 POJO)
 *   - 在 Service 里用 ProductDocument.from(src) 转成 ES 文档后灌索引
 */
@Data
public class ProductSource {

    /** 商品 ID, 跟 product.id 一致 */
    private Long id;

    /** 分类 ID */
    private Long categoryId;

    /** 商品名 */
    private String name;

    /** 简短描述 */
    private String description;

    /** 详情正文 */
    private String detail;

    /** 价格, 用 BigDecimal 防浮点精度坑 */
    private BigDecimal price;

    /** 库存 */
    private Integer stock;

    /** 销量 */
    private Integer sales;

    /** 平均评分 */
    private BigDecimal avgRating;

    /** 评论数 */
    private Integer reviewCount;

    /** 封面图 URL */
    private String coverImage;

    /** 状态 (0=下架, 1=上架), DB 是 TINYINT, Java 对应 Byte */
    private Byte status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
