package com.minimall.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 发货 DTO (G6 新增)
 *
 * 设计:
 *   - 2 个字段才包 DTO; 单字段直接裸参数 (符合 feedback_concrete_first)
 *   - @NotBlank 让 Spring Validation 帮校验, 比手写 if-else 干净
 *   - logisticsCompany 是中文"顺丰" / "中通" 等, 实际可枚举但 N=1 暂用 String
 */
@Data
public class ShipOrderDTO {

    /** 物流公司 (如: 顺丰 / 中通 / 京东) */
    @NotBlank(message = "物流公司不能为空")
    private String logisticsCompany;

    /** 物流单号 */
    @NotBlank(message = "物流单号不能为空")
    private String logisticsNo;
}
