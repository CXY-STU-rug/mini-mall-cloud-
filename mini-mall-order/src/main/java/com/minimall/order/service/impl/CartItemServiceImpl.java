package com.minimall.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.minimall.common.core.domain.Result;
import com.minimall.common.core.exception.BusinessException;
import com.minimall.order.client.ProductFeignClient;
import com.minimall.order.entity.CartItem;
import com.minimall.order.mapper.CartItemMapper;
import com.minimall.order.service.ICartItemService;
import com.minimall.order.vo.CartItemVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * CartItem 服务实现 (G3.4 核心 - 含 Feign 跨服务调用!)
 *
 * vs 单体的核心改动:
 *   ① userId 不再从 UserContext 拿, 而是【方法参数】传入
 *   ② productService 改成 productFeignClient.getById (HTTP 调 product 服务)
 *   ③ 返回的商品数据是 Map<String, Object>, 因为 order 引不到 Product 类
 *      → 取值: (BigDecimal) productMap.get("price")
 *
 * 性能说明:
 *   单体: listMyCart 里调 productService.listByIds(...) 批量查一次 → 性能最好
 *   微服务 (本实现): 循环单查 product.getById N 次 → 性能差但代码简单, 适合教学
 *   生产版: product 服务应加 batch 接口 (POST /product/batch), Feign 一次 Cake 拿全
 */
@Service
public class CartItemServiceImpl
        extends ServiceImpl<CartItemMapper, CartItem>
        implements ICartItemService {

    /**
     * Feign 客户端 - 调 product 服务
     *
     * @Autowired 注入的是 Spring 启动时给 ProductFeignClient 接口生成的【动态代理】
     * 调用代理的 getById(id) → 内部:
     *   ① Nacos 查 mini-mall-product 健康实例
     *   ② LoadBalancer 挑一个 (默认轮询)
     *   ③ 发 HTTP GET 到 http://实例:9002/product/{id}
     *   ④ 解析返回 JSON 成 Result<Map<String,Object>>
     */
    @Autowired
    private ProductFeignClient productFeignClient;

    /**
     * ① 加入购物车
     *
     * 逻辑跟单体一样:
     *   先查 (user_id, product_id) 这个 cart_item 在不在,
     *     在 → quantity 累加
     *     不在 → insert 新行
     */
    @Override
    public void addToCart(Long userId, Long productId, Integer quantity) {
        // 业务校验: 数量必须 > 0
        if (quantity == null || quantity <= 0) {
            throw new BusinessException(400, "数量必须大于 0");
        }

        // 查购物车里是否已有这个商品
        QueryWrapper<CartItem> w = new QueryWrapper<>();
        w.eq("user_id", userId).eq("product_id", productId);
        CartItem existing = this.getOne(w);    // 等价 baseMapper.selectOne(w)

        if (existing != null) {
            // 已存在 → 累加数量
            existing.setQuantity(existing.getQuantity() + quantity);
            this.updateById(existing);
        } else {
            // 不存在 → 新建
            CartItem item = new CartItem();
            item.setUserId(userId);
            item.setProductId(productId);
            item.setQuantity(quantity);
            this.save(item);
        }
    }

    /**
     * ② 我的购物车 (带商品详情)
     *
     * 流程:
     *   1) 查我的所有购物车项 (一条 SQL)
     *   2) 循环, 每条调 Feign 查商品详情 (N 次 HTTP)
     *   3) 拼 VO 返回
     *
     * 注: 循环 N 次 Feign 性能差,
     *     生产建议给 product 加 batch 接口, 这里教学先简单做
     */
    @Override
    public List<CartItemVO> listMyCart(Long userId) {
        // ── 1) 查购物车项 ──
        QueryWrapper<CartItem> w = new QueryWrapper<>();
        w.eq("user_id", userId).orderByDesc("create_time");
        List<CartItem> items = this.list(w);

        List<CartItemVO> result = new ArrayList<>();
        if (items.isEmpty()) {
            return result;      // 空车直接返
        }

        // ── 2) 循环组装 VO ──
        for (CartItem item : items) {
            // ⭐ Feign 调用 - 看起来像本地方法, 实际是 HTTP 请求
            Result<Map<String, Object>> resp = productFeignClient.getById(item.getProductId());

            // 商品不存在 / product 服务挂了 → 跳过这一条 (容错)
            if (resp == null || resp.getCode() != 200 || resp.getData() == null) {
                continue;
            }

            Map<String, Object> p = resp.getData();

            // ⭐ Map.get 返 Object, 必须强转
            //   price 在 MySQL 是 DECIMAL, Jackson 反序列化默认转 BigDecimal
            //   name 是 String 自然 String
            //   coverImage 可能 null, 不用强转
            BigDecimal price = new BigDecimal(p.get("price").toString());   // 多套层 toString 避免 Double 精度问题

            CartItemVO vo = new CartItemVO();
            vo.setCartItemId(item.getId());
            vo.setProductId(item.getProductId());
            vo.setProductName((String) p.get("name"));
            vo.setProductImage((String) p.get("coverImage"));
            vo.setPrice(price);
            vo.setQuantity(item.getQuantity());
            // ⭐ BigDecimal 必须用 .multiply, 不能用 *
            vo.setSubtotal(price.multiply(BigDecimal.valueOf(item.getQuantity())));

            result.add(vo);
        }

        return result;
    }
}
