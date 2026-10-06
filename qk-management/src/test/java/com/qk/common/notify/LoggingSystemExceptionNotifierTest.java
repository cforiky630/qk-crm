package com.qk.common.notify;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

/**
 * 占位告警出口
 * <p>
 * 这一层结构的意义是「异步 + 有界 + 丢弃也不抛」：它一定跑在正在返回 500 的路径上，
 * 告警再拥堵也不能阻塞请求线程、更不能把异常抛回调用方。
 * （队列打满后记 WARN 丢弃的策略由构造参数保证，这里不去制造 256 条日志来验证。）
 */
class LoggingSystemExceptionNotifierTest {

    @Test
    void notifyNeverThrowsAndAcceptsNull() {
        LoggingSystemExceptionNotifier notifier = new LoggingSystemExceptionNotifier();
        try {
            for (int i = 0; i < 5; i++) {
                int seq = i;
                Assertions.assertDoesNotThrow(() -> notifier.notify(new SystemAlert(
                        LocalDateTime.now(), "java.lang.IllegalStateException", "boom-" + seq, "GET", "/x", 1L)));
            }
            Assertions.assertDoesNotThrow(() -> notifier.notify(null), "null 告警应被忽略而不是抛 NPE");
        } finally {
            notifier.shutdown();
        }
    }
}
