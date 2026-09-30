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
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常：属于「预期内的失败」，HTTP 状态保持 200，由响应体 code=0 表达
     */
    @ExceptionHandler(BusinessException.class)
    public Result handlerBusinessException(BusinessException e) {
        log.warn("业务校验未通过: {}", e.getMessage());
        return Result.error(e.getMessage());
    }

    @ExceptionHandler(DuplicateKeyException.class) // 处理DuplicateKeyException类型的异常
    public Result handlerException(DuplicateKeyException e) {// 这个参数用于接收捕获到的异常
        String message = e.getMessage(); // 异常信息中包含违反的唯一索引名称
        if (message.contains("dept.name")) {
            log.error("部门名称已存在");
            return Result.error("部门名称已存在");
        }
        if (message.contains("role.label")) {
            log.error("角色标识已存在");
            return Result.error("角色标识已存在");
        }
        if (message.contains("user.username")) {
            log.error("用户名已存在");
            return Result.error("用户名已存在");
        }
        if (message.contains("user.phone")) {
            log.error("手机号已存在");
            return Result.error("手机号已存在");
        }
        if (message.contains("user.email")) {
            log.error("邮箱已存在");
            return Result.error("邮箱已存在");
        }
        if (message.contains("clue.phone")) {
            log.error("该手机号已录入线索");
            return Result.error("该手机号已录入线索");
        }
        if (message.contains("business.phone")) {
            log.error("该手机号已录入商机");
            return Result.error("该手机号已录入商机");
        }
        if (message.contains("customer.phone")) {
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
    public Result handlerMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return Result.error("请求参数格式不正确");
    }

    /**
     * 参数类型不匹配，例如路径变量该传数字却传了字母，返回 400
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result handlerTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("请求参数类型不匹配: {} = {}", e.getName(), e.getValue());
        return Result.error("请求参数类型不正确");
    }

    /**
     * 上传文件超出限制，返回 400
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result handlerMaxUploadSize(MaxUploadSizeExceededException e) {
        log.warn("上传文件超出大小限制: {}", e.getMessage());
        return Result.error("上传文件过大");
    }

    /**
     * 兜底异常：属于代码或依赖的缺陷，返回 500 并只记录日志，不把内部细节暴露给前端
     * <p>
     * 与业务异常的区别是：业务异常返回 200 + code=0，兜底异常返回 500，
     * 这样监控系统可以通过 HTTP 状态码直接识别「真正的故障」。
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result handlerException(Exception e) {
        log.error("服务器发生异常", e);
        return Result.error("系统繁忙,请稍后重试");
    }
}
