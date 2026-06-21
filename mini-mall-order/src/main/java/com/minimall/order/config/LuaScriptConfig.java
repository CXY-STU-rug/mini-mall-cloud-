package com.minimall.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

/**
 * Lua 脚本统一配置 (从单体搬, 0 改动)
 *
 * 为啥用 @Bean:
 *   ① 启动时加载一次脚本, 后续业务类直接 @Autowired 注入复用
 *   ② Spring 帮你做 SHA1 缓存 (Redis 用 EVALSHA 而不是每次都传整段脚本, 减少网络开销)
 *
 * 如果后续秒杀加更多脚本 (比如"释放库存"), 在这里追加 @Bean 就行.
 */
@Configuration
public class LuaScriptConfig {

    /**
     * 秒杀预扣库存脚本
     *
     * 文件位置: src/main/resources/lua/seckill_stock.lua
     *   走 classpath 加载, 打包进 jar 后仍能找到 (ClassPathResource 处理)
     *
     * 返回类型: Long
     *   Lua 脚本 return 数字 → Spring 转成 Long (不能是 Integer, EVAL 协议规定)
     */
    @Bean
    public DefaultRedisScript<Long> seckillStockScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(
                new ResourceScriptSource(new ClassPathResource("lua/seckill_stock.lua"))
        );
        script.setResultType(Long.class);
        return script;
    }
}
