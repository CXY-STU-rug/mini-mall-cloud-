package com.minimall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.minimall.product.entity.Product;
import com.minimall.product.mapper.ProductMapper;
import com.minimall.product.service.IProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Product Service 实现 (G3.9 从单体搬 5 方法)
 *
 * 单体 ProductServiceImpl 原方法照抄, 包名换 + Redis Bean 类型用微服务的
 */
@Service
@Slf4j
public class ProductServiceImpl
        extends ServiceImpl<ProductMapper, Product>
        implements IProductService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /** 详情: Redis 缓存 → 没中查 MySQL → 回写缓存 10 分钟 */
    @Override
    public Product getProductDetail(Long id) {
        String key = "product:detail:" + id;

        Object cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            log.info("缓存命中 key={}", key);
            return (Product) cached;
        }

        log.info("缓存未命中, 查 MySQL key={}", key);
        Product product = productMapper.selectById(id);
        if (product == null) {
            return null;
        }

        redisTemplate.opsForValue().set(key, product, 10, TimeUnit.MINUTES);
        return product;
    }

    /** 改 MySQL → 删缓存(下次详情请求会回查 + 重新写) */
    @Override
    public boolean updateProduct(Product product) {
        boolean ok = updateById(product);
        if (ok) {
            redisTemplate.delete("product:detail:" + product.getId());
            log.info("缓存已删除 key=product:detail:{}", product.getId());
        }
        return ok;
    }

    /** 删 MySQL(逻辑删) → 删缓存 */
    @Override
    public boolean deleteProduct(Long id) {
        boolean ok = removeById(id);
        if (ok) {
            redisTemplate.delete("product:detail:" + id);
            log.info("缓存已删除 key=product:detail:{}", id);
        }
        return ok;
    }

    /** 分页搜索 + 关键字写热搜 ZSet (24h 过期) */
    @Override
    public IPage<Product> searchProducts(Integer page, Integer size,
                                        Long categoryId, String keyword,
                                        BigDecimal minPrice, BigDecimal maxPrice) {
        Page<Product> pageObj = new Page<>(page, size);

        QueryWrapper<Product> w = new QueryWrapper<>();
        if (categoryId != null) w.eq("category_id", categoryId);
        if (StringUtils.hasText(keyword)) {
            w.like("name", keyword);
            redisTemplate.opsForZSet().incrementScore("hot:search", keyword, 1);
            redisTemplate.expire("hot:search", 24, TimeUnit.HOURS);
            log.info("记录热搜 keyword={}", keyword);
        }
        if (minPrice != null) w.ge("price", minPrice);
        if (maxPrice != null) w.le("price", maxPrice);
        w.orderByDesc("create_time");

        return this.page(pageObj, w);
    }

    /** 取 ZSet 倒序前 N 个 + score */
    @Override
    public List<Map<String, Object>> getHotSearch(int topN) {
        Set<ZSetOperations.TypedTuple<Object>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores("hot:search", 0, topN - 1);

        List<Map<String, Object>> result = new ArrayList<>();
        if (tuples != null) {
            for (ZSetOperations.TypedTuple<Object> t : tuples) {
                Map<String, Object> item = new HashMap<>();
                item.put("keyword", t.getValue());
                item.put("count", t.getScore());
                result.add(item);
            }
        }
        return result;
    }
}
