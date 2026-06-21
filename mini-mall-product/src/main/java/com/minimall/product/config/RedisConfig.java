package com.minimall.product.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 模板配置 (G3.5)
 *
 * ⚠️ 这是【第二份】RedisConfig (第一份在 mini-mall-order)
 *
 * 为什么不抽公共 common-redis 模块？
 *   按 feedback_concrete_first 原则: 同一痛点【重复 N 次】(N≥3) 再抽
 *   现在才 2 次, 还没触达"痛点", 等 user 服务也要用时 (第 3 次) 再抽
 *
 * 跟 order 的 RedisConfig 唯一区别: 包名不同
 *   com.minimall.order.config.RedisConfig
 *   com.minimall.product.config.RedisConfig
 *
 * 内容完全一样:
 *   - key  使用 StringRedisSerializer    → redis-cli 能直读
 *   - value 使用 GenericJackson2JsonRedisSerializer → 任意对象自动 JSON, 带 @class 类型
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        StringRedisSerializer stringSer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSer = new GenericJackson2JsonRedisSerializer();

        template.setKeySerializer(stringSer);          // 大 key
        template.setValueSerializer(jsonSer);          // String/List/Set/ZSet 的 value
        template.setHashKeySerializer(stringSer);      // Hash 的 field
        template.setHashValueSerializer(jsonSer);      // Hash 的 field 值

        template.afterPropertiesSet();
        return template;
    }
}
