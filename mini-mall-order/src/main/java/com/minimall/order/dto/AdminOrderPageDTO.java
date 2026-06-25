package com.minimall.order.dto;

import lombok.Data;

/**
 * ADMIN.5 后台订单分页查询入参
 *
 * 字段全部 optional:
 *   - page/size:   分页 (默认 1 / 20)
 *   - status:      0~4 (待付/已付/已发/完成/取消), null 不过滤
 *   - userId:      只看某用户的订单
 *   - keyword:     模糊匹配 orderNo / receiver / phone
 */
@Data
public class AdminOrderPageDTO {

    private Integer page = 1;
    private Integer size = 20;
    private Byte status;
    private Long userId;
    private String keyword;
}
