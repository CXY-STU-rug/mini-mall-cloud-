package com.minimall.order.controller;

import com.minimall.common.core.domain.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Redis 联调测试 Controller (G1.4)
 *
 * 3 个最简接口验证 Redis 通路:
 *   GET /order/redis/set/{k}/{v}  → SET k v
 *   GET /order/redis/get/{k}      → GET k
 *   GET /order/redis/del/{k}      → DEL k
 *
 * 教学目的:
 *   ① 确认 RedisConfig 的 RedisTemplate 真被 Spring 装配 (能注入就成功)
 *   ② 确认序列化器正常 (redis-cli 看到的 value 是 JSON 不是二进制乱码)
 *   ③ 端到端: curl → Controller → RedisTemplate → Lettuce → Redis Server
 */
@RestController
// ⭐ 类前缀 /order/redis: 这样跟 HelloController 的 /order 共享前缀
//   网关路由不用改, 现有 /order/** 自动覆盖
@RequestMapping("/order/redis")
public class RedisTestController {

    /**
     * 注入【我们自己配的】RedisTemplate
     * (RedisConfig 里的 @Bean redisTemplate(...) 那个)
     *
     * 注: 字段注入 @Autowired 是单元测试不友好的写法,
     *     生产推荐【构造器注入】, 教学项目用字段注入更直观
     *
     * 泛型 <String, Object>:
     *   key 是 String, value 是任意对象 (Object)
     *   跟 RedisConfig 里写的 Bean 签名要一致, 否则注入不上
     */
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * ① SET 接口
     *
     * curl http://127.0.0.1:9003/order/redis/set/test:hello/world
     * → Redis 里执行: SET "test:hello" "\"world\""
     *                 (value 是 JSON, 字符串带引号)
     *
     * opsForValue() 拿到操作【String/普通 value】的子模板,
     * 类似的还有:
     *   opsForHash()  → 操作 Hash
     *   opsForList()  → 操作 List
     *   opsForSet()   → 操作 Set
     *   opsForZSet()  → 操作 ZSet (有序集合)
     */
    @GetMapping("/set/{key}/{value}")
    public Result<String> set(@PathVariable String key,
                              @PathVariable String value) {
        // opsForValue().set(k, v): 等价 Redis 命令 SET k v
        // 注: value 类型是 Object, 所以这里传字符串也可以、传任意对象也可以
        redisTemplate.opsForValue().set(key, value);
        return Result.success("ok");
    }

    /**
     * ② GET 接口
     *
     * curl http://127.0.0.1:9003/order/redis/get/test:hello
     * → 走 Redis GET, 返 "world"
     *
     * 关键: get 返 Object, 因为 Spring 不知道你存进去的是 String 还是 User 还是 Map
     *      JSON 反序列化时, GenericJackson2JsonRedisSerializer 看 @class 字段决定还原成啥
     */
    @GetMapping("/get/{key}")
    public Result<Object> get(@PathVariable String key) {
        // 不存在的 key, get 返 null, 我们直接传给前端
        Object value = redisTemplate.opsForValue().get(key);
        return Result.success(value);
    }

    /**
     * ③ DEL 接口
     *
     * curl http://127.0.0.1:9003/order/redis/del/test:hello
     * → 走 Redis DEL, 返 true (key 存在被删) / false (key 不存在)
     *
     * 注: delete 在 RedisTemplate 上是【模板级】方法, 不需要 opsForValue,
     *     因为 DEL 命令对所有数据类型都通用
     */
    @GetMapping("/del/{key}")
    public Result<Boolean> del(@PathVariable String key) {
        // delete(k) 返 Boolean: true=删了一个, false=key 不存在
        Boolean deleted = redisTemplate.delete(key);
        return Result.success(deleted);
    }
}
