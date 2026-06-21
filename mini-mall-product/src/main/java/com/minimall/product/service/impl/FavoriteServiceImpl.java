package com.minimall.product.service.impl;

import com.minimall.product.entity.Product;
import com.minimall.product.service.IFavoriteService;
import com.minimall.product.service.IProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 收藏服务实现 (G3.5 核心)
 *
 * ⭐ 跟单体的差异:
 *   - userId 从【方法参数】传入 (单体是从 UserContext.getUserId() 隐式拿)
 *   - 调 productService.getById() 替代单体的 getProductDetail() (功能等价)
 *
 * Redis 数据结构: Set
 *   key   = "favorite:user:{userId}"           (一个用户一个 key)
 *   value = Set<Long> productIds              (Set 天然去重 + O(1) 判断成员)
 *
 * 为什么选 Set 不选 List?
 *   - 去重: 同一个商品收藏 N 次还是一份, 业务正好
 *   - 成员判断快: SISMEMBER O(1), List 要 LRANGE 全扫
 *   - 集合运算: 后面想做"猜你喜欢"可以 SINTER 求两人共同收藏
 *
 * 为什么 value 不直接存 Product 对象?
 *   - 商品信息会变 (改价/改名), 存快照会脏数据
 *   - 只存 id, 用时实时去查【最新】商品
 */
@Service
public class FavoriteServiceImpl implements IFavoriteService {

    /**
     * Redis 模板 (来自 RedisConfig.redisTemplate Bean)
     * 注: 这里类型是 <String, Object>, 跟 Bean 声明完全一致
     */
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 复用本服务的商品 Service (同一个 JVM, 直接 @Autowired, 不用 Feign)
     *
     * ⚠️ 这就是为什么我们把 Favorite 放 product 服务:
     *   如果放 user 服务, 调商品就要 Feign → HTTP → 慢一截又复杂
     *   放 product 同进程, getById 直接走本地内存调用, 等于 0 网络成本
     */
    @Autowired
    private IProductService productService;

    /**
     * 构造 Redis key
     *
     * 命名规范: 业务名 + ":" + 维度名 + ":" + 维度值
     * 例:  favorite:user:1  →  alice 的收藏列表
     *      favorite:user:2  →  bob 的收藏列表
     *
     * 这种约定让 redis-cli `KEYS favorite:*` 能找出所有收藏 key
     */
    private String key(Long userId) {
        return "favorite:user:" + userId;
    }

    @Override
    public void add(Long userId, Long productId) {
        // opsForSet().add(k, v...) 等价 Redis 命令: SADD k v1 v2 ...
        // 返 Long = 真正加进去的元素数 (已存在的不算)
        redisTemplate.opsForSet().add(key(userId), productId);
    }

    @Override
    public void remove(Long userId, Long productId) {
        // SREM k v
        redisTemplate.opsForSet().remove(key(userId), productId);
    }

    @Override
    public List<Product> listMy(Long userId) {
        // ① SMEMBERS 取所有成员
        // 注: SMEMBERS 在大 Set 上慢, 生产建议用 SSCAN 游标遍历
        //     这里教学项目用户最多收藏几十个, 直接 SMEMBERS 够用
        Set<Object> productIds = redisTemplate.opsForSet().members(key(userId));

        List<Product> result = new ArrayList<>();
        if (productIds == null || productIds.isEmpty()) {
            return result;   // 空收藏直接返空列表, 不查 product 表
        }

        // ② 循环查商品详情
        //
        // ⚠️ 为什么不用 productService.listByIds() 批量查?
        //    可以用! 这里偷懒循环单查, 方便对照单体实现
        //    生产版应该改成批量, 减少 SQL 次数 (N → 1)
        //
        // ⚠️ productIds 元素类型是 Object (因为 RedisTemplate 泛型是 Object)
        //    需要先转 Long 再传给 getById
        //    GenericJackson2JsonRedisSerializer 反序列化数字默认是 Integer/Long
        //    所以 toString() 后再 Long.valueOf 是【最保险】写法
        for (Object pidObj : productIds) {
            Long pid = Long.valueOf(pidObj.toString());
            Product p = productService.getById(pid);
            if (p != null) {
                result.add(p);    // 已删除的商品自动跳过, 容错
            }
        }

        return result;
    }

    @Override
    public boolean isFavorited(Long userId, Long productId) {
        // SISMEMBER k v → Boolean
        // 注: 返 Boolean 可能是 null (Redis 异常), 这里用 Boolean.TRUE.equals 双保险
        Boolean is = redisTemplate.opsForSet().isMember(key(userId), productId);
        return Boolean.TRUE.equals(is);
    }
}
