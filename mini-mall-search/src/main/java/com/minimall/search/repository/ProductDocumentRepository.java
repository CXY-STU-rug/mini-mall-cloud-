package com.minimall.search.repository;

import com.minimall.search.document.ProductDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * 商品 ES 文档 Repository (相当于 ES 版的 MyBatis-Plus BaseMapper)
 * <p>
 * 设计说明:
 *   - 继承 ElasticsearchRepository&lt;T, ID&gt; → Spring Data 启动时自动用 JDK 动态代理
 *     生成实现类, 不需要写任何方法体
 *   - 自动获得 20+ 个 CRUD 方法:
 *       save(doc)        — upsert (有则更新, 无则插入), 不是单纯 insert
 *       saveAll(list)    — 批量灌, 同步全量数据时用
 *       findById(id)     — 按主键查
 *       findAll()        — 全量查 (慎用, 数据量大会爆内存)
 *       findAll(Pageable)— 分页查
 *       deleteById(id)   — 按主键删
 *       count()          — 索引文档总数
 *   - 泛型参数:
 *       T  = ProductDocument  (你的 @Document 类)
 *       ID = Long             (ProductDocument.id 字段的 Java 类型, 必须用包装类)
 * <p>
 * 用法: 在 Service 里 @Resource 注入, 直接调上面这些方法.
 */
public interface ProductDocumentRepository extends ElasticsearchRepository<ProductDocument, Long> {
    // 不需要任何方法! Spring Data 启动时自动生成实现类
    // 后续如果有"按某字段查询"的复杂需求, 可以在这里加方法签名
    // 比如: List<ProductDocument> findByCategoryId(Long categoryId);
    // Spring Data 看到方法名带 findBy 也会自动生成实现 (派生查询)
}
