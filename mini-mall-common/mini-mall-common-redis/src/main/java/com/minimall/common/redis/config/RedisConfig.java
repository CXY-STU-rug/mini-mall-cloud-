package com.minimall.common.redis.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 公共 Redis 配置 (G7 重构后抽出 N=3 触发)
 * <p>
 * ════════════════════════════════════════════════════════════════
 * 来源说明:
 *   - G1.3 在 order 服务建了第一份 RedisConfig
 *   - G3.5.3 在 product 服务建了第二份 (G3.9 修过, 加了 JavaTimeModule)
 *   - 现在 review 即将用 Redis → N=3, 终于到 feedback_concrete_first 阈值, 抽出来
 * <p>
 * 跟原版 (product/RedisConfig.java) 唯一区别:
 *   ⭐ 加了 @ConditionalOnMissingBean → 业务服务自己想覆盖时直接定义一个同名 Bean
 *      不强行替换, 保留逃生口
 * <p>
 * 4 个序列化器位置 (复习一下 G1 学过的):
 *   - keySerializer       大 key → StringRedisSerializer (UTF-8 直读)
 *   - valueSerializer     String/List/Set/ZSet 的 value → JSON
 *   - hashKeySerializer   Hash 的 field 名 → String
 *   - hashValueSerializer Hash 的 field 值 → JSON
 * ════════════════════════════════════════════════════════════════
 */
@Configuration
public class RedisConfig {

    /**
     * 自定义 RedisTemplate
     * <p>
     * 解决两个 Spring Boot 默认 RedisTemplate 的痛点:
     *   ① 默认 JDK 序列化 → redis-cli 看到全是乱码
     *   ② 默认 ObjectMapper 不带 JavaTimeModule → 存 LocalDateTime 直接爆
     */
    @Bean
    @ConditionalOnMissingBean(name = "redisTemplate")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // ─── 序列化器准备 ────────────────────────────────────
        StringRedisSerializer stringSer = new StringRedisSerializer();

        // ⭐ 手动配 ObjectMapper, 注册 JavaTimeModule 解决 LocalDateTime 问题
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        // 让 Jackson 能看到所有字段 (包括 private), 默认只看 public getter
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // activateDefaultTyping: 序列化时多写一个 @class 字段, 反序列化时根据 @class 还原原类型
        //   LaissezFaireSubTypeValidator: 不限制能反序列化哪些类 (生产应该用白名单)
        //   NON_FINAL: 给所有非 final 类加 @class
        om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL);

        GenericJackson2JsonRedisSerializer jsonSer = new GenericJackson2JsonRedisSerializer(om);

        // ─── 4 个位置分别设 ───────────────────────────────
        template.setKeySerializer(stringSer);
        template.setValueSerializer(jsonSer);
        template.setHashKeySerializer(stringSer);
        template.setHashValueSerializer(jsonSer);

        // 必调! 不调 Spring 用默认配置
        template.afterPropertiesSet();
        return template;
    }
}
