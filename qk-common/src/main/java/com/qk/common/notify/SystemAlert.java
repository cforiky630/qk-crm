package com.qk.common.notify;

import java.time.LocalDateTime;

/**
 * 系统异常告警内容
 * <p>
 * 只装「运维排障需要知道的最小信息」：什么时候、谁、打了哪个接口、抛了什么异常。
 * 刻意不带请求体、不带堆栈 —— 那些留在应用日志里，告警通道要短要能一眼扫完。
 * <p>
 * 这里只收纯值，不依赖 {@code HttpServletRequest}：把请求拆成方法与路径是 Web 层的事，
 * 否则这个通道抽象就被绑死在 Servlet 上，也没法从定时任务之类的非 Web 场景发起告警。
 *
 * @param occurredAt       发生时间
 * @param exceptionType    异常类型全限定名
 * @param exceptionMessage 异常信息
 * @param httpMethod       请求方法，非 Web 场景为 null
 * @param requestUri       请求路径，非 Web 场景为 null
 * @param operatorId       当前登录用户ID，未登录时为 null
 */
public record SystemAlert(LocalDateTime occurredAt,
                          String exceptionType,
                          String exceptionMessage,
                          String httpMethod,
                          String requestUri,
                          Long operatorId) {

    /**
     * 从异常与请求信息组装告警
     *
     * @param e           未预期的异常
     * @param httpMethod  请求方法，可以为 null
     * @param requestUri  请求路径，可以为 null
     * @param operatorId  当前登录用户ID，可以为 null
     * @return 告警内容
     */
    public static SystemAlert of(Exception e, String httpMethod, String requestUri, Long operatorId) {
        return new SystemAlert(LocalDateTime.now(), e.getClass().getName(), e.getMessage(),
                httpMethod, requestUri, operatorId);
    }
}
