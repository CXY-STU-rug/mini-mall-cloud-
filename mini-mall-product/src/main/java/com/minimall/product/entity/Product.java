package com.minimall.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体（product 表的镜像）
 *
 * 跟单体 mini-mall 里的 Product.java 一模一样，只改了包名
 */
@Getter
@Setter
public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 分类 ID */
    private Long categoryId;

    /** 商品名 */
    private String name;

    /** 简短描述 */
    private String description;

    /** 详情（富文本 HTML） */
    private String detail;

    /** 价格（元） */
    private BigDecimal price;

    /** 库存 */
    private Integer stock;

    /** 销量（冗余字段） */
    private Integer sales;

    /** 封面图 URL */
    private String coverImage;

    /** 状态：0 下架 / 1 上架 */
    private Byte status;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Byte isDeleted;
}
