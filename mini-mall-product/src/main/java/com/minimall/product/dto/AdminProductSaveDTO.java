package com.minimall.product.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * ADMIN.4 新增 / 编辑商品入参 (共用一个 DTO)
 *
 * 设计:
 *   - 新增时 id=null, MP 自增填上
 *   - 编辑时 id 必填, controller 用 saveOrUpdate() 一招通吃
 *   - 校验注解(@NotBlank/@Min/@DecimalMin) 配合 controller 上的 @Validated 自动报 400
 */
@Data
public class AdminProductSaveDTO {

    /** 编辑时必填, 新增时不传 */
    private Long id;

    @NotBlank(message = "商品名不能为空")
    @Size(max = 100, message = "商品名最长 100 字")
    private String name;

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于 0")
    private BigDecimal price;

    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存不能为负")
    private Integer stock;

    /** 简短描述, 可空 */
    private String description;

    /** 详情 HTML, 可空 (ADMIN 阶段先不接富文本) */
    private String detail;

    /** 封面图 URL, 可空 (后续接 OSS 上传) */
    private String coverImage;

    /** 0 下架 / 1 上架, 默认下架更安全 */
    private Byte status = 0;
}
