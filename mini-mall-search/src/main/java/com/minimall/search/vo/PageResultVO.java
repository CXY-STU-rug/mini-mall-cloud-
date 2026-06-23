package com.minimall.search.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 通用分页结果 VO (泛型类)
 * <p>
 * 泛型 T = "每条记录的类型". 用 PageResultVO&lt;ProductSearchVO&gt; 就装商品,
 * 用 PageResultVO&lt;OrderVO&gt; 就装订单. 一个类多种用途.
 * <p>
 * 不直接用 MyBatis-Plus 的 IPage 是因为 search 服务没引 MP 依赖.
 */
@Data
@AllArgsConstructor   // 5 字段全参构造, service 里 new PageResultVO(total, pages, page, size, records) 直接传
public class PageResultVO<T> {

    /** 总条数 — 来自 ES 的 totalHits, 比如搜"华为"匹配 1234 条 */
    private Long total;

    /** 总页数 — total/size 向上取整, 比如 1234 条/每页 10 条 = 124 页 */
    private Long pages;

    /** 当前是第几页 — 跟前端传的 page 一致, 从 1 开始 */
    private Integer page;

    /** 每页几条 — 跟前端传的 size 一致, 默认 10 */
    private Integer size;

    /** 当前页的数据列表 — 类型是泛型 T (用时填具体类型如 ProductSearchVO) */
    private List<T> records;
}
