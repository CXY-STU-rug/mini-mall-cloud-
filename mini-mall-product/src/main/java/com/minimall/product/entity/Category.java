package com.minimall.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品分类表 (从单体 com.minimall.minimall.entity 搬过来)
 *
 * 迁移变化:
 *   - 包名: com.minimall.minimall.entity → com.minimall.product.entity
 *   - 其他 0 改动 (Entity 是【纯数据载体】, 跟微服务架构无关)
 */
@Getter
@Setter
public class Category implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     * IdType.AUTO = 用数据库自增, 入库时 MP 自动回填到 entity
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 分类名 */
    private String name;

    /** 图标 URL */
    private String icon;

    /** 排序值, 越小越靠前 */
    private Integer sort;

    /** 0=禁用, 1=启用 */
    private Byte status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记 (0=未删 1=已删)
     * @TableLogic: MP 看到这字段, 自动把 delete 改成 update set is_deleted=1
     *              select 自动加 where is_deleted = 0
     */
    @TableLogic
    private Byte isDeleted;
}
