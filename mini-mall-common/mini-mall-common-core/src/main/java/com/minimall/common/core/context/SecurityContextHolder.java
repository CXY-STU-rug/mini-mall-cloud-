package com.minimall.common.core.context;

import com.alibaba.ttl.TransmittableThreadLocal;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户上下文持有者
 *
 * 作用：在【当前线程】里随时拿到当前登录用户信息（userId / userName / 等）
 *
 * 工作原理：
 *   ① 请求进入业务服务 → 拦截器从 HTTP Header 读 user_id → 调 setUserId() 塞进 TTL
 *   ② Controller/Service 任何地方 → 调 getUserId() 取出
 *   ③ 请求结束 → 拦截器调 remove() 清理（不清 ThreadLocal 会内存泄漏！）
 *
 * 为什么用 Map 而不是直接存 Long userId？
 *   未来可能要存 userName/tenantId/roles 等多个字段，Map 扩展灵活
 *   每个字段一个 ThreadLocal 会很乱，统一一个 Map 装
 */
public class SecurityContextHolder {

    // ─── ThreadLocal 容器：每个线程一份独立的 Map ─────────────────
    // 用 TransmittableThreadLocal 不用 ThreadLocal，原因见类注释
    private static final TransmittableThreadLocal<Map<String, Object>> CONTEXT
            = new TransmittableThreadLocal<>() {
        @Override
        protected Map<String, Object> initialValue() {
            // 首次 get() 时如果还没 set，自动给个空 Map，避免 NPE
            return new HashMap<>();
        }
    };

    // ─── Key 常量（先就近放这，等多了再抽 SecurityConstants） ─────
    public static final String USER_ID_KEY = "user_id";
    public static final String USER_NAME_KEY = "user_name";

    // ═══════════════════════════════════════════════════════════
    // 通用 set/get（操作 Map）
    // ═══════════════════════════════════════════════════════════

    /**
     * 往当前线程的 Map 里塞一个 key-value
     * TODO 你来写：
     *   1. 从 CONTEXT.get() 拿当前线程的 Map
     *   2. 把 key-value 放进 Map
     *   3. 把 Map set 回 CONTEXT（其实不 set 也行，因为是同一引用，但写上更清晰）
     */
    public static void set(String key, Object value) {
      CONTEXT.get().put(key, value);
    }

    /**
     * 从当前线程的 Map 里取值
     * TODO 你来写：
     *   1. 从 CONTEXT.get() 拿 Map
     *   2. 用 map.get(key) 取值
     *   3. 如果是 null 返回 ""（避免下游 NPE）
     */
    public static String get(String key) {
        // 从当前线程的 Map 里取 key 对应的值（Object 类型，可能是 String/Long/Integer 等）
        Object value = CONTEXT.get().get(key);
        // 没取到（key 不存在）返回空串而非 null，让上游调用方不用判 null
        return value == null ? "" : value.toString();
    }
    // ═══════════════════════════════════════════════════════════
    // 业务便捷方法（封装 set/get，业务代码用这些）
    // ═══════════════════════════════════════════════════════════

    /** 拿当前登录用户 id；没登录返回 0 */
    public static Long getUserId() {
        String userIdStr = get(USER_ID_KEY);
        // 字符串转 Long，空串/null 返回 0
        return userIdStr.isEmpty() ? 0L : Long.parseLong(userIdStr);
    }

    /** 设置当前登录用户 id（拦截器里调） */
    public static void setUserId(String userId) {
        set(USER_ID_KEY, userId);
    }

    /** 拿当前登录用户名 */
    public static String getUserName() {
        return get(USER_NAME_KEY);
    }

    /** 设置当前登录用户名 */
    public static void setUserName(String userName) {
        set(USER_NAME_KEY, userName);
    }

    // ═══════════════════════════════════════════════════════════
    // 清理（防内存泄漏）
    // ═══════════════════════════════════════════════════════════

    /**
     * 清当前线程的上下文
     * ⭐⭐⭐ 拦截器 afterCompletion 里必须调，否则线程池复用时会拿到上次的用户信息
     */
    public static void remove() {
        CONTEXT.remove();
    }
}