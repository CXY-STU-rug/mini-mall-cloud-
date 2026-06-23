package com.minimall.search.vo;

import com.minimall.search.document.ProductDocument;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品搜索结果摘要 VO (返给前端)
 * <p>
 * 设计原则: 只返"卡片展示需要的字段", 不返 description/detail/stock/status/time
 * (那些详情页才用, 搜索列表不展示 → 省带宽 + 前端代码更清晰).
 */
@Data
public class ProductSearchVO {

    /**
     * 商品 ID — 前端点击卡片跳详情 /product/{id}
     */
    private Long id;

    /**
     * 商品名 — 卡片标题
     */
    private String name;

    /**
     * 价格 — 卡片显示 ¥X,XXX, 用 BigDecimal 防浮点精度坑
     */
    private BigDecimal price;

    /**
     * 封面图 URL — 卡片缩略图
     */
    private String coverImage;

    /**
     * 分类 ID — 卡片显示分类标签 (前端拿 id 再查分类名)
     */
    private Long categoryId;

    /**
     * 销量 — 卡片显示"已售 1.2 万"
     */
    private Integer sales;

    /**
     * 平均评分 — 卡片显示"⭐ 4.8 分"
     */
    private BigDecimal avgRating;

    /**
     * 评论数 — 卡片显示"(1234 条评论)"
     */
    private Integer reviewCount;

    public static ProductSearchVO from(ProductDocument doc) {
        ProductSearchVO vo = new ProductSearchVO();
        vo.setId(doc.getId());
        vo.setName(doc.getName());
        vo.setPrice(doc.getPrice());
        vo.setCoverImage(doc.getCoverImage());
        vo.setCategoryId(doc.getCategoryId());
        vo.setSales(  doc.getSales());
        vo.setAvgRating(doc.getAvgRating());
        vo.setReviewCount(doc.getReviewCount());
        return vo;
    }
}