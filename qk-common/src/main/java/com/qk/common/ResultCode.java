package com.qk.common;

import lombok.Getter;

/**
 * 统一响应码
 * <p>
 * 只做「魔法值收敛」，取值与既有对外契约完全一致：
 * {@code 1} 表示成功、{@code 0} 表示失败。
 * 不引入任何细分错误码，接口文档与前端判断逻辑均不受影响。
 * <p>
 * 失败的具体原因由 {@link Result#error(String)} 的 msg 表达，例如「客户不存在」。
 */
@Getter
public enum ResultCode {

    /** 成功 */
    SUCCESS(1, "success"),

    /** 失败：默认提示语，实际响应一般由调用方传入更具体的中文提示 */
    FAIL(0, "操作失败");

    private final int code;
    private final String msg;

    ResultCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
