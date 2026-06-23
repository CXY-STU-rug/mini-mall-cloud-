package com.minimall.search.service;

import com.minimall.search.dto.ProductSearchRequest;
import com.minimall.search.vo.PageResultVO;
import com.minimall.search.vo.ProductSearchVO;
/**
 * 商品搜索服务接口 (search 服务对外契约)
 * <p>
 * 4 个核心能力:
 *   1. syncAll       — 全量同步, 把 product 服务的所有商品灌进 ES (首次部署/索引重建)
 *   2. syncById      — 单条同步, 商品上架/编辑后增量更新 (MQ 通知触发)
 *   3. deleteById    — 单条删除, 商品下架/删除时从索引移除
 *   4. search        — 搜索, 关键词 + 分类 + 价格区间 + 分页 + 排序
 * <p>
 * 设计原则:
 *   - 接口在这里, 实现在 service/impl/ProductSearchServiceImpl
 *   - Controller 只依赖这个接口, 不依赖实现 (Spring 自动注入实现 bean)
 *   - sync 系列方法是"写 ES", search 是"读 ES", 读写分离清晰
 */
public interface IProductSearchService {


    /**
     * 全量同步: 从 product 服务拉所有商品 → 灌进 ES.
     * 用途: 首次部署 / 索引重建 / 数据修复.
     * @return 同步了多少条
     */
    int syncAll();

    /**
     * 单条同步: 把某个商品的最新数据更新进 ES (upsert).
     * 用途: 商品上架/编辑/价格变动 → MQ 通知 search 服务来同步.
     * @param productId 商品 ID
     */
    void syncById(Long productId);

    /**
     * 单条删除: 把商品从 ES 索引里删掉.
     * 用途: 商品下架/删除.
     * @param productId 商品 ID
     */
    void deleteById(Long productId);

    /**
     * 搜索接口: 按关键词/分类/价格区间搜商品, 支持分页和排序.
     * @param request 搜索条件 (你之前写的 ProductSearchRequest)
     * @return 分页结果 (你之前写的 PageResultVO<ProductSearchVO>)
     */
    PageResultVO<ProductSearchVO> search(ProductSearchRequest request);

}
