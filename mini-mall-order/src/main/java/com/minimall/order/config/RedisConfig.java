package com.minimall.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 模板配置 (G1 核心)
 * <p>
 * 干一件事: 用我们自己定义的 RedisTemplate【替换】Spring Boot 默认的那个,
 *           解决【默认 JDK 序列化】导致 redis-cli 看到乱码的问题。
 * <p>
 * ⭐ 4 个序列化器的概念:
 * <pre>
 *   redisTemplate.opsForValue().set("user:1", new User(1, "alice"));
 *           │                       │              │
 *           │                       │              └─ value: 序列化成什么?
 *           │                       └─ key: 序列化成什么?
 *           └─ 模板自己
 *
 *   redisTemplate.opsForHash().put("cart:1", "p101", 2);
 *           │                       │         │      │
 *           │                       │         │      └─ hash value
 *           │                       │         └─ hash key (field 名)
 *           │                       └─ key (大 key)
 *           └─ 模板自己
 *
 *   一共 4 个位置可以独立配序列化器:
 *     ① keySerializer       ← 大 key
 *     ② valueSerializer     ← String/List/Set/ZSet 的 value
 *     ③ hashKeySerializer   ← Hash 的 field 名
 *     ④ hashValueSerializer ← Hash 的 field 值
 * </pre>
 * <p>
 * 我们的选择:
 *   key  → StringRedisSerializer        (UTF-8 字符串, redis-cli 能直读)
 *   value → GenericJackson2JsonRedisSerializer (JSON, 带类型信息)
 *
 * 这样存什么对象都能转 JSON, 取回来 Spring 自动反序列回原类型。
 */
@Configuration
public class RedisConfig {

    /**
     * 自定义 RedisTemplate Bean
     * <p>
     * @Bean 标记: Spring 注册成 Bean, 名字默认是方法名 "redisTemplate"
     *            刚好替换掉 Spring Boot 默认的同名 Bean
     * <p>
     * RedisConnectionFactory: 由 Spring Boot 自动装配 (Lettuce),
     *                         我们通过参数注入, 不用自己 new
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {

        // ① 创建模板对象
        RedisTemplate<String, Object> template = new RedisTemplate<>();

        // ② 把连接工厂塞进去 (用来获取 Redis 连接)
        template.setConnectionFactory(factory);

        // ③ ────── 两个核心序列化器 ──────
        //
        // StringRedisSerializer:
        //   把 String 用 UTF-8 直接转 byte[], 啥都不包装
        //   存 Redis 看到的就是【原文字符串】, 比如 "user:1"
        StringRedisSerializer stringSer = new StringRedisSerializer();
        //
        // GenericJackson2JsonRedisSerializer:
        //   用 Jackson 把对象序列化成 JSON, 但会【包一个 @class 字段】
        //   带类型信息, 取出来 Spring 能反序列化回原类型 (不丢类型)
        //   redis-cli 看到的是: {"@class":"com.xx.User","id":1,"name":"alice"}
        //   缺点: 多了 @class 字段, 暴露类全限定名 (生产可能不想暴露)
        GenericJackson2JsonRedisSerializer jsonSer = new GenericJackson2JsonRedisSerializer();

        // ④ ────── 4 个位置分别设 ──────
        template.setKeySerializer(stringSer);          // 大 key 用 String
        template.setValueSerializer(jsonSer);          // String/List/Set/ZSet 的 value 用 JSON
        template.setHashKeySerializer(stringSer);      // Hash 的 field 名用 String
        template.setHashValueSerializer(jsonSer);      // Hash 的 field 值用 JSON

        // ⑤ 必调!!! 让上面的设置生效, 否则用 Spring 默认的
        //   (Spring 内部会检查 initialized 标记)
        template.afterPropertiesSet();

        return template;
    }
}
