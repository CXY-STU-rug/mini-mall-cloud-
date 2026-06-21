package com.minimall.common.core.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 通用响应封装
 *
 * 后端所有接口的返回值，统一长这样：
 *   { "code": 200, "message": "操作成功", "data": {...} }
 *
 * 前端只看 code：
 *   200 → 成功，读 data
 *   其他 → 失败，弹 message
 *
 * @param <T> 业务数据类型，比如 User / List<Product> / Long(id)
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Result<T> implements Serializable {

    // ─── 3 个字段 ──────────────────────────────────────────

    /** 状态码（200=成功；其他=失败） */
    private Integer code;

    /** 响应消息（成功时是"操作成功"；失败时是错误描述给前端弹） */
    private String message;

    /** 业务数据（成功时装 User/List/id 等；失败时通常为 null） */
    private T data;

    // ─── 静态工厂方法（业务代码主要用这些） ─────────────────

    /**
     * 成功，不带数据
     * 场景：删除接口、修改接口 —— 前端只关心"成不成功"
     * 用法：return Result.success();
     */
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }

    /**
     * 成功，带数据
     * 场景：查询接口、新增接口（返回 id） —— 前端要读 data
     * 用法：return Result.success(user);
     *      return Result.success(activity.getId());
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    /**
     * 失败，自定义错误信息（默认 code=500）
     * 场景：业务异常 catch 后包装
     * 用法：return Result.error("库存不足");
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }

    /**
     * 失败，自定义 code 和 message
     * 场景：需要区分错误类型（401未登录 / 403无权限 / 400参数错）
     * 用法：return Result.error(401, "请先登录");
     */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }
}
