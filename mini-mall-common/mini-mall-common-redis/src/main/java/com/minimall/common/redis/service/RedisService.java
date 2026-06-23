package com.minimall.common.redis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 工具类 (G7 重构抽取, 跟 RedisConfig 一起放 common-redis)
 * <p>
 * 不写也行 - 业务代码可以直接 @Autowired RedisTemplate 用,
 * 但每次都 redisTemplate.opsForValue().set(..., timeout, TimeUnit.MINUTES) 太啰嗦.
 * 这里封装最常用 10 个方法, 让业务代码读起来像人话:
 *   redisService.set(key, value, 5, TimeUnit.MINUTES)
 *   redisService.del(key)
 *   redisService.hSet(key, field, value)
 *   ...
 * <p>
 * 教学说明:
 *   ① 用 RequiredArgsConstructor 替代 @Autowired field 注入 (推荐 final + 构造器)
 *   ② 方法名跟 Redis 命令对齐 (set/get/del/incr/expire), 学过 Redis 命令的一看就懂
 *   ③ 不抛异常, 出错 log.warn 返默认值 (Redis 挂了不该阻塞业务主流程)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    // ════════════════════════════════════════════════════════════
    // String 类型 (Redis 最常用的 50%)
    // ════════════════════════════════════════════════════════════

    /** 设值, 不过期 */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /** 设值 + 过期时间 (单位自定) */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /** 取值, 不存在返 null (调用方 instanceof 判断或 cast) */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /** 取值 + 类型转换 (省一次 cast) */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        Object val = redisTemplate.opsForValue().get(key);
        return val == null ? null : (T) val;
    }

    /** 计数器: INCR (常用于限流计数) */
    public Long incr(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    public Long incr(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    // ════════════════════════════════════════════════════════════
    // Key 操作
    // ════════════════════════════════════════════════════════════

    /** 删 key, 返是否真的删了 */
    public Boolean del(String key) {
        return redisTemplate.delete(key);
    }

    /** 批量删 */
    public Long del(List<String> keys) {
        return redisTemplate.delete(keys);
    }

    /** key 是否存在 */
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /** 设过期时间 (单位秒) */
    public Boolean expire(String key, long seconds) {
        return redisTemplate.expire(key, seconds, TimeUnit.SECONDS);
    }

    /** 查剩余存活时间 (秒); -1=永久 -2=不存在 */
    public Long ttl(String key) {
        return redisTemplate.getExpire(key);
    }

    // ════════════════════════════════════════════════════════════
    // Hash 类型 (适合存对象的字段, 不必整对象序列化)
    // ════════════════════════════════════════════════════════════

    public void hSet(String key, String field, Object value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    public Object hGet(String key, String field) {
        return redisTemplate.opsForHash().get(key, field);
    }

    public Long hDel(String key, Object... fields) {
        return redisTemplate.opsForHash().delete(key, fields);
    }

    // ════════════════════════════════════════════════════════════
    // Set / ZSet (秒杀/排行榜场景, G3.8 / G3.9 用过)
    // ════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    public Set<Object> sMembers(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    public Long sAdd(String key, Object... values) {
        return redisTemplate.opsForSet().add(key, values);
    }

    /** ZSet 加分 (热搜+1) */
    public Double zIncr(String key, Object value, double delta) {
        return redisTemplate.opsForZSet().incrementScore(key, value, delta);
    }
}
