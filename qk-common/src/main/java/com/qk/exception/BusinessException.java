package com.qk.exception;

/**
 * 业务异常
 * <p>
 * 用于表达「请求本身没问题，但业务规则不允许」的情况，例如数据不存在、状态不允许流转、
 * 上传文件类型不合法等。由 GlobalExceptionHandler 统一转换为
 * {@code {code: 0, msg: 具体原因}} 返回给前端，与代码缺陷（NullPointerException 之类）区分开：
 * 后者统一返回 500 且不向前端暴露细节。
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
