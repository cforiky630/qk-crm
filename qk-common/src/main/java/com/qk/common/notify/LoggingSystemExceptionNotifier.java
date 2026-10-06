package com.qk.common.notify;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 系统异常告警的默认实现：只把告警写进日志
 * <p>
 * 这里把「异步」这一层结构搭好（有界队列 + 守护线程 + 拒绝时记日志丢弃），
 * 接真实通道时只需要替换 {@link #send(SystemAlert)}：钉钉/企业微信群机器人、邮件、
 * 监控平台的 webhook 都是在这个方法里发一个请求。
 * <p>
 * 「引入即有一份可用的兜底」和 OssTemplate、OssConfig 是同一类东西，所以默认实现也放在本模块。
 * 接真实通道时，在应用里实现 {@link SystemExceptionNotifier} 并加 {@code @Primary}
 * （或直接替换本类）即可，调用方代码一行不用改。
 * <p>
 * 为什么自带线程池而不是用 {@code @Async}：告警不是业务逻辑，不该和业务共用线程池；
 * 而且它必须在「线程池满」时选择丢弃而不是阻塞 —— 发生系统异常时请求线程正紧张，
 * 绝不能因为发告警把线程占住。丢弃时留一条 WARN，避免静默丢告警查不出来。
 */
@Slf4j
@Component
public class LoggingSystemExceptionNotifier implements SystemExceptionNotifier {

    /** 待发告警队列上限：告警是尽力而为，堆积说明下游通道出问题了，不无限占用内存 */
    private static final int QUEUE_CAPACITY = 256;

    private static final AtomicInteger THREAD_SEQ = new AtomicInteger();

    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(QUEUE_CAPACITY),
            runnable -> {
                Thread thread = new Thread(runnable, "sys-alert-" + THREAD_SEQ.incrementAndGet());
                // 守护线程：残留的告警任务不应该拖住 JVM 退出
                thread.setDaemon(true);
                return thread;
            },
            (runnable, pool) -> log.warn("系统异常告警队列已满（{}），本条告警被丢弃", QUEUE_CAPACITY));

    @Override
    public void notify(SystemAlert alert) {
        if (alert == null) {
            return;
        }
        try {
            executor.execute(() -> send(alert));
        } catch (Exception e) {
            // 提交失败也不许影响调用方：调用方正在返回 500，不能再被告警拖累
            log.warn("提交系统异常告警失败", e);
        }
    }

    /**
     * 默认实现：把告警写进日志
     * <p>
     * 接入真实通道时替换这个方法即可，{@link #notify(SystemAlert)} 的异步与丢弃策略不用动。
     *
     * @param alert 告警内容
     */
    private void send(SystemAlert alert) {
        log.error("[系统异常告警] 时间={} 操作人={} 请求={} {} 异常={}: {}",
                alert.occurredAt(), alert.operatorId(), alert.httpMethod(), alert.requestUri(),
                alert.exceptionType(), alert.exceptionMessage());
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }
}
