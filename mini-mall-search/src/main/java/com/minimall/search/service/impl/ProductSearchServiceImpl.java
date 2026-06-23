package com.minimall.search.service.impl;


import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.json.JsonData;
import com.minimall.common.core.domain.Result;
import com.minimall.search.client.ProductFeignClient;
import com.minimall.search.document.ProductDocument;
import com.minimall.search.dto.ProductSearchRequest;
import com.minimall.search.entity.ProductSource;
import com.minimall.search.repository.ProductDocumentRepository;
import com.minimall.search.service.IProductSearchService;
import com.minimall.search.vo.PageResultVO;
import com.minimall.search.vo.ProductSearchVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
/**
 * 商品搜索服务实现 (search 服务核心业务逻辑)
 * <p>
 * 职责分两类:
 *   - 写 ES (syncAll/syncById/deleteById): 把 product 服务的商品数据灌进 ES 索引
 *   - 读 ES (search): 给前端提供搜索能力
 * <p>
 * 关键依赖:
 *   - repository: Spring Data ES 的 DAO, 屏蔽底层 HTTP 调用
 *   - productFeignClient: 调 product 服务拉全量商品数据
 * <p>
 * 注意:
 *   - syncAll 用 saveAll, ES 的 save 是 upsert (id 已存在会覆盖, 不冲突)
 *   - search 暂未实现, G9.4.4 再填
 */
@Service
@Slf4j

public class ProductSearchServiceImpl implements IProductSearchService {

    @Resource
    private ProductDocumentRepository repository;  // ES 的 DAO (你刚写的)

    @Resource
    private ProductFeignClient productFeignClient; // 调 product 服务 (你刚写的)

    @Resource
    private ElasticsearchOperations elasticsearchOperations; // 复杂搜索 (Repository 不够用时用它)

    @Override
    public int syncAll() {
        Result<List<ProductSource>>result = productFeignClient.listAllForSync();
        if (result.getCode() == null || result.getCode() != 200) {
            log.error("[search-sync] 拉商品失败, message={}", result.getMessage());
            return 0;
        }
        List<ProductSource> sources = result.getData();
        List<ProductDocument> documents = sources.stream()
                .map(ProductDocument::from)
                .toList();

        // 4. 批量灌进 ES (saveAll 是 upsert: 有则覆盖, 无则插入)
        repository.saveAll(documents);

        log.info("[search-sync] 全量同步完成, 共 {} 条", documents.size());
        return documents.size();
    }
    @Override
    public void syncById(Long productId) {
        log.info("syncById 还没实现, productId={}", productId);
        return;
    }

    @Override
    public void deleteById(Long productId) {  repository.deleteById(productId);
        log.info("已从 ES 删除商品 productId={}", productId);
    }

    /**
     * 搜索商品 — ES 核心方法.
     * <p>
     * 5 步流程:
     *   1. 处理 page/size 默认值 (防 null/0)
     *   2. 构造 BoolQuery (拼 keyword/categoryId/price 三类条件)
     *   3. 包装 NativeQuery (加分页 + 排序)
     *   4. ElasticsearchOperations.search() 真正执行
     *   5. SearchHits → PageResultVO 转换返回
     */
    @Override
    public PageResultVO<ProductSearchVO> search(ProductSearchRequest request) {
        // ─── 1. 参数默认值 ────────────────────────────────────────
        int page = request.getPage() == null || request.getPage() < 1 ? 1 : request.getPage();
        int size = request.getSize() == null || request.getSize() < 1 ? 10 : request.getSize();

        // ─── 2. 构造 BoolQuery ───────────────────────────────────
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        // 2a. keyword: 模糊匹配 name + description + detail (要打分, 用 must)
        if (StringUtils.hasText(request.getKeyword())) {
            Query keywordQuery = MultiMatchQuery.of(m -> m
                    .query(request.getKeyword())
                    .fields("name", "description", "detail")
            )._toQuery();
            boolBuilder.must(keywordQuery);
        }

        // 2b. categoryId: 精确过滤 (不打分, 用 filter, 有缓存更快)
        if (request.getCategoryId() != null) {
            Query catQuery = TermQuery.of(t -> t
                    .field("categoryId")
                    .value(request.getCategoryId())
            )._toQuery();
            boolBuilder.filter(catQuery);
        }

        // 2c. price 区间: gte/lte 都用 filter (精确范围过滤, 不打分)
        if (request.getMinPrice() != null || request.getMaxPrice() != null) {
            Query priceQuery = RangeQuery.of(r -> {
                r.field("price");
                if (request.getMinPrice() != null) {
                    r.gte(JsonData.of(request.getMinPrice()));
                }
                if (request.getMaxPrice() != null) {
                    r.lte(JsonData.of(request.getMaxPrice()));
                }
                return r;
            })._toQuery();
            boolBuilder.filter(priceQuery);
        }

        // ─── 3. 包装成 NativeQuery (含分页 + 排序) ──────────────
        // PageRequest 的 page 从 0 开始, 所以传 page - 1
        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(boolBuilder.build()._toQuery())
                .withPageable(PageRequest.of(page - 1, size, parseSort(request.getSort())))
                .build();

        // ─── 4. 执行查询 ────────────────────────────────────────
        SearchHits<ProductDocument> hits = elasticsearchOperations.search(nativeQuery, ProductDocument.class);

        // ─── 5. SearchHits → PageResultVO<ProductSearchVO> ─────
        List<ProductSearchVO> records = hits.getSearchHits().stream()
                .map(SearchHit::getContent)             // SearchHit → ProductDocument
                .map(ProductSearchVO::from)             // ProductDocument → ProductSearchVO (你要写的)
                .toList();

        long total = hits.getTotalHits();               // 总命中数
        long pages = (total + size - 1) / size;         // 总页数 = 向上取整(total/size)

        log.info("[search] keyword={}, total={}, page={}/{}",
                request.getKeyword(), total, page, pages);

        return new PageResultVO<>(total, pages, page, size, records);
    }

    /**
     * 解析前端传的 sort 字符串 → Spring Sort 对象.
     * <p>
     * 支持 5 种:
     *   price_asc    — 价格升序
     *   price_desc   — 价格降序
     *   sales_desc   — 销量降序
     *   rating_desc  — 评分降序
     *   newest       — 最新上架 (createTime 降序)
     *   (其他/null)  — 默认按 ES 相关度评分排序 (Sort.unsorted)
     */
    private Sort parseSort(String sort) {
        if (!StringUtils.hasText(sort)) {
            return Sort.unsorted();  // 默认按 ES 评分排序
        }
        return switch (sort) {
            case "price_asc"   -> Sort.by(Sort.Order.asc("price"));
            case "price_desc"  -> Sort.by(Sort.Order.desc("price"));
            case "sales_desc"  -> Sort.by(Sort.Order.desc("sales"));
            case "rating_desc" -> Sort.by(Sort.Order.desc("avgRating"));
            case "newest"      -> Sort.by(Sort.Order.desc("createTime"));
            default            -> Sort.unsorted();
        };
    }

}
