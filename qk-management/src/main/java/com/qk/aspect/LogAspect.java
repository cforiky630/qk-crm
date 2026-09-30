package com.qk.aspect;

import com.qk.entity.OperateLog;
import com.qk.service.OperateLogService;
import com.qk.common.util.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * 操作日志切面
 * <p>
 * 拦截标注了 {@link com.qk.aspect.anno.LogOperation} 的方法，把「谁、什么时间、调用了哪个类的哪个方法、
 * 传了什么参数、返回了什么、耗时多少」记录到 operate_log 表。
 */
@Slf4j
@Aspect
@Component
public class LogAspect {

    /** method_params 列长度上限 */
    private static final int MAX_PARAMS_LENGTH = 1000;
    /** return_value 列长度上限 */
    private static final int MAX_RETURN_LENGTH = 2000;

    /**
     * 需要脱敏的字段。
     * 同时匹配 Lombok toString 的 password=xxx 和 JSON 的 "password":"xxx" 两种写法。
     * 操作日志会把方法参数落库，而 User 这类对象带密码字段（toString 会输出它），
     * 不脱敏就等于把明文密码写进了日志表。
     */
    private static final Pattern SENSITIVE_FIELD =
            Pattern.compile("(\"?password\"?\\s*[=:]\\s*)([^,\\]}\\s]+)");

    private final OperateLogService operateLogService;

    public LogAspect(OperateLogService operateLogService) {
        this.operateLogService = operateLogService;
    }

    @Around("@annotation(com.qk.aspect.anno.LogOperation)")
    public Object aroundAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodParams = truncate(maskSensitive(Arrays.toString(joinPoint.getArgs())), MAX_PARAMS_LENGTH);

        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable e) {
            // 方法执行失败时只记录应用日志：此时业务事务通常已标记回滚，写库没有意义
            log.warn("操作失败: {}.{}，参数: {}",
                    joinPoint.getSignature().getDeclaringTypeName(), joinPoint.getSignature().getName(), methodParams, e);
            throw e;
        }

        long costTime = System.currentTimeMillis() - startTime;

        OperateLog operateLog = new OperateLog();
        operateLog.setOperateUserId(UserHolder.getCurrentUser());
        operateLog.setOperateTime(LocalDateTime.now());
        // 使用签名声明类，避免拿到 CGLIB 代理类的名字（xxx$$SpringCGLIB$$0）
        operateLog.setClassName(joinPoint.getSignature().getDeclaringTypeName());
        operateLog.setMethodName(joinPoint.getSignature().getName());
        operateLog.setMethodParams(methodParams);
        operateLog.setReturnValue(truncate(result == null ? null : maskSensitive(result.toString()), MAX_RETURN_LENGTH));
        operateLog.setCostTime(costTime);

        try {
            operateLogService.saveLog(operateLog);
        } catch (Exception e) {
            // 记日志失败不能影响正常业务流程
            log.error("保存操作日志失败", e);
        }
        return result;
    }

    /**
     * 把密码之类的敏感字段值替换成 ***，避免明文进日志表
     */
    private String maskSensitive(String text) {
        return text == null ? null : SENSITIVE_FIELD.matcher(text).replaceAll("$1***");
    }

    /**
     * 截断超长文本，避免超出数据库列长度导致插入失败
     */
    private String truncate(Object value, int maxLength) {
        if (value == null) {
            return null;
        }
        String str = value.toString();
        return str.length() <= maxLength ? str : str.substring(0, maxLength);
    }
}
