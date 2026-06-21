package com.minimall.user.config;

import com.alibaba.csp.sentinel.datasource.ReadableDataSource;
import com.alibaba.csp.sentinel.datasource.nacos.NacosDataSource;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Sentinel 规则持久化到 Nacos (F2.9 核心)
 * <p>
 * 干两件事:
 *   ① 启动时去 Nacos 拉规则配置, 加载到 Sentinel 内存
 *   ② 订阅 Nacos 配置变化, 改了规则实时推过来, 不用重启
 * <p>
 * Nacos 上配置项规划:
 *   - mini-mall-user-flow-rules.json     ← FlowRule 列表
 *   - mini-mall-user-degrade-rules.json  ← DegradeRule 列表
 * <p>
 * ⭐ 关键: 配了这个之后, Sentinel Dashboard 上配规则【只是临时改】
 *    服务重启就丢回 Nacos 里的值。生产环境应该直接改 Nacos。
 */
@Configuration
public class SentinelNacosConfig {

    // ─── Nacos 配置(跟 F1 用同一个 Nacos Server) ─────────────
    private static final String NACOS_SERVER  = "127.0.0.1:8848";
    private static final String GROUP         = "SENTINEL_GROUP";   // 跟 application yml 分组隔开

    // ─── 配置项 dataId(每种规则一个文件) ──────────────────────
    private static final String FLOW_DATA_ID    = "mini-mall-user-flow-rules.json";
    private static final String DEGRADE_DATA_ID = "mini-mall-user-degrade-rules.json";

    @PostConstruct
    public void init() {
        loadFlowRules();
        loadDegradeRules();
        System.out.println("=========== Sentinel 规则已从 Nacos 加载 ===========");
    }

    /**
     * 加载限流规则
     *
     * 步骤:
     *   1) 创建 NacosDataSource (一个【数据源】, 内部维护跟 Nacos 的长连接)
     *      构造参数: nacos地址 + group + dataId + 反序列化函数
     *
     *   2) 用 FlowRuleManager.register2Property() 把数据源【挂到】Sentinel 规则中心
     *      之后:
     *        - 启动时立即拉一次, 内容加载到 FlowRuleManager 内存里
     *        - Nacos 配置变化时, NacosDataSource 收到 push → 通知 FlowRuleManager 重新加载
     */
    private void loadFlowRules() {
        // source 是 Nacos 配置项的【纯字符串内容】(我们存的是 JSON 数组字符串)
        // 反序列化: 字符串 → List<FlowRule>
        // 用 fastjson (Sentinel 内置依赖, 不需要额外引)
        ReadableDataSource<String, List<FlowRule>> flowDataSource = new NacosDataSource<>(
                NACOS_SERVER,
                GROUP,
                FLOW_DATA_ID,
                source -> JSON.parseObject(source, new TypeReference<List<FlowRule>>() {})
        );

        // register2Property: 把数据源的 Property 注册给规则中心
        // 数据源的 Property 一变化, FlowRuleManager 自动 loadRules
        FlowRuleManager.register2Property(flowDataSource.getProperty());
    }

    /**
     * 加载熔断规则 (跟限流一模一样的套路)
     */
    private void loadDegradeRules() {
        ReadableDataSource<String, List<DegradeRule>> degradeDataSource = new NacosDataSource<>(
                NACOS_SERVER,
                GROUP,
                DEGRADE_DATA_ID,
                source -> JSON.parseObject(source, new TypeReference<List<DegradeRule>>() {})
        );
        DegradeRuleManager.register2Property(degradeDataSource.getProperty());
    }
}
