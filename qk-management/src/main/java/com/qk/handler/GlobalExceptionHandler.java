package com.qk.handler;

import com.qk.common.Result;
import com.qk.common.exception.BusinessException;
import com.qk.common.exception.ErrorCode;
import com.qk.common.util.UserHolder;
import com.qk.common.notify.SystemAlert;
import com.qk.common.notify.SystemExceptionNotifier;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;
import java.util.Objects;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final SystemExceptionNotifier systemExceptionNotifier;

    public GlobalExceptionHandler(SystemExceptionNotifier systemExceptionNotifier) {
        this.systemExceptionNotifier = systemExceptionNotifier;
    }

    /**
     * 业务异常：属于「预期内的失败」，HTTP 状态保持 200，由响应体 code=0 表达
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handlerBusinessException(BusinessException e) {
        log.warn("业务校验未通过: [{}] {}", e.getErrorCode(), e.getMessage());
        return Result.error(e.getMessage());
    }

    /** 「表.唯一索引名」→ 冲突提示；键即 MySQL 报错里的索引全名 */
    private static final Map<String, ErrorCode> UNIQUE_KEY_ERRORS = Map.ofEntries(
            Map.entry("dept.uk_name", ErrorCode.DEPT_NAME_EXISTS),
            Map.entry("role.uk_label", ErrorCode.ROLE_LABEL_EXISTS),
            Map.entry("user.uk_username", ErrorCode.USER_USERNAME_EXISTS),
            Map.entry("user.uk_phone", ErrorCode.USER_PHONE_EXISTS),
            Map.entry("user.uk_email", ErrorCode.USER_EMAIL_EXISTS),
            Map.entry("clue.uk_phone", ErrorCode.CLUE_PHONE_EXISTS),
            Map.entry("business.uk_phone", ErrorCode.BUSINESS_PHONE_EXISTS),
            Map.entry("customer.uk_phone", ErrorCode.CUSTOMER_PHONE_EXISTS));

    /**
     * 唯一索引冲突：「表.唯一索引名」→ 用户能看懂的提示
     * <p>
     * 唯一索引统一命名为 {@code uk_列名}，MySQL 的重复键报错里带的就是这个索引名，
     * 形如 {@code Duplicate entry 'x' for key 'user.uk_username'}。
     * 新增一个唯一索引时，只需要在下面这张表里补一行。
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handlerDuplicateKey(DuplicateKeyException e) {
        String message = e.getMessage();
        if (message != null) {
            for (Map.Entry<String, ErrorCode> entry : UNIQUE_KEY_ERRORS.entrySet()) {
                if (message.contains(entry.getKey())) {
                    ErrorCode errorCode = entry.getValue();
                    log.error("唯一索引冲突 [{}]: {}", entry.getKey(), errorCode.getMessage());
                    return Result.error(errorCode.getMessage());
                }
            }
        }
        // 没识别出来说明库里有没登记的唯一索引，日志留证，对外只给通用提示
        log.error("未识别的唯一索引冲突: {}", message);
        return Result.error(ErrorCode.UNKNOWN_FAILURE.getMessage());
    }

    /**
     * 请求体解析失败：JSON 格式错误、日期格式不合法等，属于客户端问题，返回 400
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handlerMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return Result.error(ErrorCode.PARAM_FORMAT_INVALID.getMessage());
    }

    /**
     * 参数类型不匹配，例如路径变量该传数字却传了字母，返回 400
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handlerTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("请求参数类型不匹配: {} = {}", e.getName(), e.getValue());
        return Result.error(ErrorCode.PARAM_TYPE_INVALID.getMessage());
    }

    /**
     * DTO 校验失败（@Valid + @NotBlank/@Min 等注解）
     * <p>
     * 按项目既有约定返回 HTTP 200 + code=0：这里属于「可预期的业务失败」，
     * 与 Service 层抛 BusinessException 的表现一致；HTTP 400 只保留给
     * 「请求体根本不是合法 JSON」「路径参数类型不匹配」这类框架层解析错误。
     * <p>
     * msg 取第一条字段错误信息，具体文案由 DTO 注解上的 message 决定。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handlerValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> Objects.toString(fieldError.getDefaultMessage(), null))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(ErrorCode.PARAM_VALIDATION_FAILED.getMessage());
        log.warn("参数校验未通过: {}", message);
        return Result.error(message);
    }

    /**
     * 上传文件超出限制，返回 400
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handlerMaxUploadSize(MaxUploadSizeExceededException e) {
        log.warn("上传文件超出大小限制: {}", e.getMessage());
        return Result.error(ErrorCode.UPLOAD_SIZE_EXCEEDED.getMessage());
    }

    /**
     * 兜底异常：属于代码或依赖的缺陷，返回 500 并只记录日志，不把内部细节暴露给前端
     * <p>
     * 与业务异常的区别是：业务异常返回 200 + code=0，兜底异常返回 500，
     * 这样监控系统可以通过 HTTP 状态码直接识别「真正的故障」。
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handlerException(Exception e, HttpServletRequest request) {
        log.error("服务器发生异常", e);
        alertSystemException(e, request);
        return Result.error(ErrorCode.SYSTEM_ERROR.getMessage());
    }

    /**
     * 异步告警运维
     * <p>
     * 业务异常是预期内的失败，返回提示即可；系统异常是代码或依赖的缺陷，必须让运维知道 ——
     * 否则只有等用户投诉才会发现。这里把异常交给告警出口就返回，不等结果、不看结果。
     * <p>
     * {@link SystemExceptionNotifier#notify} 约定不抛异常，这里仍再包一层：
     * 兜底处理器是最后一道防线，它自己抛出异常会连 500 的响应体都丢掉，多一层 try 是廉价的保险。
     */
    private void alertSystemException(Exception e, HttpServletRequest request) {
        try {
            systemExceptionNotifier.notify(SystemAlert.of(e,
                    request == null ? null : request.getMethod(),
                    request == null ? null : request.getRequestURI(),
                    UserHolder.getCurrentUser()));
        } catch (Exception alertFailure) {
            log.warn("系统异常告警发送失败", alertFailure);
        }
    }
}
