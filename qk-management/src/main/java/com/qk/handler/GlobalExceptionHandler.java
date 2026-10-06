package com.qk.handler;

import com.qk.common.Result;
import com.qk.common.exception.BusinessException;
import com.qk.common.exception.ErrorCode;
import com.qk.common.exception.ForbiddenException;
import com.qk.common.util.UserHolder;
import com.qk.common.notify.SystemAlert;
import com.qk.common.notify.SystemExceptionNotifier;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

    /**
     * 已登录但角色不满足接口要求：返回 403
     * <p>
     * 与业务失败（HTTP 200 + code = 0）区分开、与 401（未登录，响应体为空）配套：
     * 前端据此提示「没有权限」而不是「请重新登录」。比 BusinessException 更具体，优先命中。
     */
    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handlerForbidden(ForbiddenException e) {
        log.warn("无权限访问: {}", e.getMessage());
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
        String message = firstFieldMessage(e.getBindingResult());
        log.warn("参数校验未通过: {}", message);
        return Result.error(message);
    }

    /**
     * 查询参数（{@code @ModelAttribute} 风格的对象入参）校验失败
     * <p>
     * {@code @RequestBody} 的校验失败抛 {@link MethodArgumentNotValidException}，
     * 查询对象的校验失败抛 {@link BindException} —— 两者都是「入参不合法」，
     * 因此共用同一套提示逻辑（前者是后者的子类，Spring 会优先命中更具体的处理器）。
     * <p>
     * 分页参数（{@code ?page=} 传空串、{@code page=0}、{@code pageSize=99999}）就走这条路径：
     * 改造前它会变成 500 或静默返回空列表，现在返回 code = 0 + 具体字段提示。
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handlerBindException(BindException e) {
        String message = firstFieldMessage(e.getBindingResult());
        log.warn("参数绑定校验未通过: {}", message);
        return Result.error(message);
    }

    /**
     * 数据库完整性约束不满足（列超长、非空、类型不匹配等）
     * <p>
     * 根因是「入参超出了库里的约束」，属于可预期的客户端输入问题，不该变成 500 并触发运维告警。
     * {@link DuplicateKeyException} 是它的子类，已有更具体的处理器，不会走到这里。
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result<Void> handlerDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("数据完整性约束不满足: {}", e.getMostSpecificCause().getMessage());
        return Result.error(ErrorCode.DATA_INTEGRITY_VIOLATION.getMessage());
    }

    /**
     * 请求了不存在的路径：返回 404，而不是兜底的 500
     * <p>
     * 兜底处理器 {@code @ExceptionHandler(Exception.class)} 的优先级高于 Spring 默认的
     * {@code DefaultHandlerExceptionResolver}，如果不显式接住，任何 404 都会变成
     * 「500 + 系统繁忙」并触发运维告警 —— 客户端写错 URL 不该算服务端故障。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handlerNoResourceFound(NoResourceFoundException e) {
        log.warn("请求的资源不存在: {} {}", e.getHttpMethod(), e.getResourcePath());
        return Result.error(ErrorCode.RESOURCE_NOT_FOUND.getMessage());
    }

    /** 请求方法不被支持（例如对 {@code /login} 发 GET）：返回 405 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<Void> handlerMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不被支持: {}", e.getMethod());
        return Result.error(ErrorCode.METHOD_NOT_ALLOWED.getMessage());
    }

    /** 请求头 Content-Type 不被支持（例如给 JSON 接口发 text/plain）：返回 415 */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public Result<Void> handlerMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        log.warn("请求的 Content-Type 不被支持: {}", e.getContentType());
        return Result.error(ErrorCode.CONTENT_TYPE_UNSUPPORTED.getMessage());
    }

    /** 缺少必填的请求参数：返回 400 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handlerMissingParameter(MissingServletRequestParameterException e) {
        log.warn("缺少请求参数: {}", e.getParameterName());
        return Result.error(ErrorCode.PARAM_MISSING.format(e.getParameterName()));
    }

    /**
     * 缺少 multipart 请求分片（例如 {@code POST /upload} 没带 image 字段）：返回 400
     * <p>
     * 与「缺少查询参数」同一类问题：都属于请求本身不完整，不该算服务端故障。
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handlerMissingPart(MissingServletRequestPartException e) {
        log.warn("缺少请求体分片: {}", e.getRequestPartName());
        return Result.error(ErrorCode.PARAM_MISSING.format(e.getRequestPartName()));
    }

    /**
     * 其它带状态码的框架异常：保留框架给出的状态码，不要统一降级成 500
     * <p>
     * Spring 的 {@code ErrorResponse} 体系（{@code ResponseStatusException} 等）已经表达了
     * 正确的 4xx/5xx 语义，兜底处理器不应把它们抹平成「500 + 系统繁忙」。
     */
    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<Result<Void>> handlerErrorResponse(ErrorResponseException e) {
        int status = e.getStatusCode().value();
        log.warn("请求被框架拒绝: {} {}", status, e.getBody().getDetail());
        ErrorCode code = status >= 500 ? ErrorCode.SYSTEM_ERROR : ErrorCode.PARAM_VALIDATION_FAILED;
        return ResponseEntity.status(e.getStatusCode()).body(Result.error(code.getMessage()));
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

    /** 取第一条字段级校验提示；没有字段提示时回落到统一文案 */
    private static String firstFieldMessage(BindingResult bindingResult) {
        return bindingResult.getFieldErrors().stream()
                .map(fieldError -> Objects.toString(fieldError.getDefaultMessage(), null))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(ErrorCode.PARAM_VALIDATION_FAILED.getMessage());
    }
}
