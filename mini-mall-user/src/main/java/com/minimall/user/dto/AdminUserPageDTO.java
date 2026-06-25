package com.minimall.user.dto;

import lombok.Data;

/**
 * 后台用户列表查询参数 (ADMIN.3)
 *
 * Spring MVC 默认从 query string 把每个字段映射进来,
 * 例如 /admin/user/page?page=1&size=20&keyword=alice → 自动填好.
 */
@Data
public class AdminUserPageDTO {
    /** 第几页 (从 1 开始) */
    private Long page = 1L;

    /** 每页几条 */
    private Long size = 20L;

    /** 关键词 - 模糊匹配 username / nickname */
    private String keyword;

    /** 状态过滤 - 0=禁用 1=正常; null=不过滤 */
    private Integer status;

    /** 角色过滤 - 0=普通 1=管理员; null=不过滤 */
    private Integer role;
}
