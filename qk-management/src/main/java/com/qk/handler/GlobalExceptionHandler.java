package com.qk.handler;

import com.qk.common.Result;
import com.qk.common.exception.BusinessException;
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

import java.util.Objects;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常：属于「预期内的失败」，HTTP 状态保持 200，由响应体 code=0 表达
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handlerBusinessException(BusinessException e) {
        log.warn("业务校验未通过: {}", e.getMessage());
        return Result.error(e.getMessage());
    }

    /**
     * 唯一索引冲突：把数据库异常翻译成用户能看懂的提示
     * <p>
     * 唯一索引统一命名为 {@code uk_列名}，MySQL 的重复键报错里带的就是这个索引名，
     * 形如 {@code Duplicate entry 'x' for key 'user.uk_username'}，
     * 据此把冲突定位到具体字段，才能给出「用户名已存在」这类可读提示。
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handlerDuplicateKey(DuplicateKeyException e) {
        String message = e.getMessage();
        if (violatesUniqueKey(message, "dept", "name")) {
            log.error("部门名称已存在");
            return Result.error("部门名称已存在");
        }
        if (violatesUniqueKey(message, "role", "label")) {
            log.error("角色标识已存在");
            return Result.error("角色标识已存在");
        }
        if (violatesUniqueKey(message, "user", "username")) {
            log.error("用户名已存在");
            return Result.error("用户名已存在");
        }
        if (violatesUniqueKey(message, "user", "phone")) {
            log.error("手机号已存在");
            return Result.error("手机号已存在");
        }
        if (violatesUniqueKey(message, "user", "email")) {
            log.error("邮箱已存在");
            return Result.error("邮箱已存在");
        }
        if (violatesUniqueKey(message, "clue", "phone")) {
            log.error("该手机号已录入线索");
            return Result.error("该手机号已录入线索");
        }
        if (violatesUniqueKey(message, "business", "phone")) {
            log.error("该手机号已录入商机");
            return Result.error("该手机号已录入商机");
        }
        if (violatesUniqueKey(message, "customer", "phone")) {
            log.error("该手机号已录入客户");
            return Result.error("该手机号已录入客户");
        }
        return Result.error("操作失败,请联系管理员");
    }

    /**
     * 请求体解析失败：JSON 格式错误、日期格式不合法等，属于客户端问题，返回 400
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handlerMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return Result.error("请求参数格式不正确");
    }

    /**
     * 参数类型不匹配，例如路径变量该传数字却传了字母，返回 400
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handlerTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("请求参数类型不匹配: {} = {}", e.getName(), e.getValue());
        return Result.error("请求参数类型不正确");
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
                .orElse("请求参数校验未通过");
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
        return Result.error("上传文件过大");
    }

    /**
     * 判断异常信息里的唯一索引是否为 {@code 表.列}
     * <p>
     * 索引名形如 {@code uk_列名}；这里同时认不带前缀的写法，
     * 避免索引改名后提示悄悄退化成「操作失败,请联系管理员」。
     *
     * @param message 数据库异常信息
     * @param table   表名
     * @param column  列名
     * @return 命中的唯一索引属于该列时返回 true
     */
    private static boolean violatesUniqueKey(String message, String table, String column) {
        if (message == null) {
            return false;
        }
        return message.contains(table + "." + column) || message.contains(table + ".uk_" + column);
    }

    /**
     * 兜底异常：属于代码或依赖的缺陷，返回 500 并只记录日志，不把内部细节暴露给前端
     * <p>
     * 与业务异常的区别是：业务异常返回 200 + code=0，兜底异常返回 500，
     * 这样监控系统可以通过 HTTP 状态码直接识别「真正的故障」。
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handlerException(Exception e) {
        log.error("服务器发生异常", e);
        return Result.error("系统繁忙,请稍后重试");
    }
}
