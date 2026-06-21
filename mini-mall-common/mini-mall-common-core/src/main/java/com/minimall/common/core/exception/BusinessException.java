package com.minimall.common.core.exception;

import lombok.Getter;

/**
 * 业务异常
 *
 * 触发场景：业务规则违反
 *   - 库存不足 / 余额不足
 *   - 用户名密码错误
 *   - 活动未开始 / 已结束
 *   - 重复操作（已收藏过/已秒杀过）
 *
 * 抛出方式：业务代码里直接 throw，不用 catch（GlobalExceptionHandler 兜底翻译成 Result）
 *
 * 为什么继承 RuntimeException 而不是 Exception？
 *   RuntimeException 是【非受检异常】，不强制方法签名写 throws
 *   Exception 是【受检异常】，每层方法都得 throws，污染代码
 *   业务异常 99% 都是 RuntimeException
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 业务状态码（前端用它区分错误类型，比如 401 跳登录页） */
    private final Integer code;

    /**
     * 构造方法 1：只传 message，code 默认 500
     * 用法：throw new BusinessException("库存不足");
     */
    public BusinessException(String message) {
        // 调父类 RuntimeException(message) 构造，让 getMessage() 能拿到
        super(message);
        this.code = 500;
    }

    /**
     * 构造方法 2：自定义 code 和 message
     * 用法：throw new BusinessException(401, "请先登录");
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
