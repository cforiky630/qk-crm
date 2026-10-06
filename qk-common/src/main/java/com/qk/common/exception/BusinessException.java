package com.qk.common.exception;

import java.util.Objects;

/**
 * 业务异常
 * <p>
 * 用于表达「请求本身没问题，但业务规则不允许」的情况，例如数据不存在、状态不允许流转、
 * 上传文件类型不合法等。异常只携带 {@link ErrorCode} 与实际文案：
 * 文案统一在错误码枚举里维护，这里不再接受散落的字符串，避免提示语又散回各处。
 * <p>
 * 由 GlobalExceptionHandler 统一转换为 {@code {code: 0, msg: 具体原因}} 返回给前端，
 * 与代码缺陷（NullPointerException 之类）区分开：后者统一返回 500 且不向前端暴露细节。
 */
public class BusinessException extends RuntimeException {

    /** 触发的业务错误码，便于结构化日志与将来按类型区分返回 */
    private final ErrorCode errorCode;

    /**
     * @param errorCode 业务错误码，展示文案取自它
     * @param args      文案模板占位对应的实参；没有占位时不传
     */
    public BusinessException(ErrorCode errorCode, Object... args) {
        super(Objects.requireNonNull(errorCode, "业务错误码不能为空").format(args));
        this.errorCode = errorCode;
    }

    /**
     * 保留根因的构造器
     * <p>
     * 上传、远程调用这类失败，如果只留一句「读取上传文件失败」，日志里看不出到底是超时、
     * 被拒绝还是断流。带上原始异常只是为了排查，它不会出现在返回给前端的 {@code msg} 里。
     *
     * @param errorCode 业务错误码，展示文案取自它
     * @param cause     根因，仅进日志
     * @param args      文案模板占位对应的实参；没有占位时不传
     */
    public BusinessException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(Objects.requireNonNull(errorCode, "业务错误码不能为空").format(args), cause);
        this.errorCode = errorCode;
    }

    /** @return 触发的业务错误码 */
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
