package com.qk;

import com.qk.common.Result;
import com.qk.handler.GlobalExceptionHandler;
import com.qk.common.notify.SystemAlert;
import com.qk.common.notify.SystemExceptionNotifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 系统异常告警
 * <p>
 * 业务异常有统一提示就够了；系统异常（兜底 500）是代码或依赖的缺陷，必须让运维知道，
 * 否则只有等用户投诉才发现。这里守住三件事：告警确实发出去、走 HTTP 时参数解析正常、
 * 以及告警失败不能把错误响应本身弄坏。
 */
class GlobalExceptionHandlerTest {

    /** 记录调用的假告警出口，避免测试依赖真实通道 */
    private static final class RecordingNotifier implements SystemExceptionNotifier {

        private final List<SystemAlert> alerts = new ArrayList<>();

        @Override
        public void notify(SystemAlert alert) {
            alerts.add(alert);
        }
    }

    /** 专门用来触发兜底 500 的测试用控制器 */
    @RestController
    static class BoomController {

        @GetMapping(value = "/boom", produces = MediaType.APPLICATION_JSON_VALUE)
        public String boom() {
            throw new IllegalStateException("数据库连接池耗尽");
        }
    }

    @Test
    void systemExceptionIsAlertedAndResponseIsUnchanged() {
        RecordingNotifier notifier = new RecordingNotifier();
        GlobalExceptionHandler handler = new GlobalExceptionHandler(notifier);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/clues");

        Result<Void> result = handler.handlerException(new IllegalStateException("数据库连接池耗尽"), request);

        Assertions.assertEquals(0, result.getCode());
        Assertions.assertEquals("系统繁忙,请稍后重试", result.getMsg(), "对外的提示不能变");

        Assertions.assertEquals(1, notifier.alerts.size(), "系统异常必须告警一次");
        SystemAlert alert = notifier.alerts.get(0);
        Assertions.assertEquals(IllegalStateException.class.getName(), alert.exceptionType());
        Assertions.assertEquals("数据库连接池耗尽", alert.exceptionMessage());
        Assertions.assertEquals("POST", alert.httpMethod());
        Assertions.assertEquals("/clues", alert.requestUri());
        Assertions.assertNotNull(alert.occurredAt());
    }

    /**
     * 走一次真实的 MVC 处理链：兜底处理器带了 {@code HttpServletRequest} 参数，
     * 直接调用测不出 Spring 能不能解析它，必须过一遍请求。
     */
    @Test
    void systemExceptionOverHttpReturns500AndAlerts() throws Exception {
        RecordingNotifier notifier = new RecordingNotifier();
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new BoomController())
                .setControllerAdvice(new GlobalExceptionHandler(notifier))
                .build();

        mockMvc.perform(get("/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("系统繁忙,请稍后重试"));

        Assertions.assertEquals(1, notifier.alerts.size());
        Assertions.assertEquals("/boom", notifier.alerts.get(0).requestUri());
        Assertions.assertEquals("GET", notifier.alerts.get(0).httpMethod());
    }

    @Test
    void alertingFailureDoesNotBreakTheErrorResponse() {
        // 告警通道挂了也必须照样返回 500 的响应体，不能再抛出去
        GlobalExceptionHandler handler = new GlobalExceptionHandler(alert -> {
            throw new IllegalStateException("告警通道不可用");
        });

        Result<Void> result = handler.handlerException(new RuntimeException("boom"), new MockHttpServletRequest("GET", "/x"));

        Assertions.assertEquals(0, result.getCode());
        Assertions.assertEquals("系统繁忙,请稍后重试", result.getMsg());
    }
}
