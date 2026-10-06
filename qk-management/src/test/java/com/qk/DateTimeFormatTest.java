package com.qk;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;

/**
 * 时间格式契约
 * <p>
 * 守住两件事：全局默认格式（对外契约），以及「字段级 {@code @JsonFormat} 覆盖」这个出口。
 * 后者曾经是失效的：自定义序列化器不实现 contextual，字段上写 pattern 完全没有效果，
 * 而且静默 —— 不报错、不告警。
 */
@SpringBootTest
class DateTimeFormatTest {

    /** 只放一个字段，便于断言完整报文 */
    static class Plain {
        public LocalDateTime plain;
    }

    static class Sample {
        public LocalDateTime plain;

        @JsonFormat(pattern = "yyyy-MM-dd")
        public LocalDateTime dayOnly;

        @JsonFormat(pattern = "yyyy年MM月dd日 HH时mm分")
        public LocalDateTime chinese;
    }

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void defaultOutputFormatIsLocked() {
        Plain holder = new Plain();
        holder.plain = LocalDateTime.of(2026, 6, 1, 9, 30, 0);

        Assertions.assertEquals("{\"plain\":\"2026-06-01 09:30:00\"}", jsonMapper.writeValueAsString(holder),
                "全局输出格式是对外契约，改动要同步 README 与 docs/openapi.yaml");
    }

    @Test
    void fieldLevelPatternOverridesOutput() {
        LocalDateTime value = LocalDateTime.of(2026, 6, 1, 9, 30, 0);
        Sample sample = new Sample();
        sample.plain = value;
        sample.dayOnly = value;
        sample.chinese = value;

        String json = jsonMapper.writeValueAsString(sample);

        Assertions.assertTrue(json.contains("\"dayOnly\":\"2026-06-01\""), json);
        Assertions.assertTrue(json.contains("\"chinese\":\"2026年06月01日 09时30分\""), json);
        Assertions.assertTrue(json.contains("\"plain\":\"2026-06-01 09:30:00\""),
                "没标注解的字段仍用全局格式: " + json);
    }

    @Test
    void fieldLevelPatternOverridesInput() {
        Sample parsed = jsonMapper.readValue("""
                {"plain":"2026-06-01 09:30:00","dayOnly":"2026-06-01","chinese":"2026年06月01日 09时30分"}
                """, Sample.class);

        Assertions.assertEquals(LocalDateTime.of(2026, 6, 1, 9, 30), parsed.plain);
        Assertions.assertEquals(LocalDateTime.of(2026, 6, 1, 0, 0), parsed.dayOnly,
                "只给日期的字段级格式按当天零点处理");
        Assertions.assertEquals(LocalDateTime.of(2026, 6, 1, 9, 30), parsed.chinese);
    }

    @Test
    void lenientInputFormatsStillWork() {
        Assertions.assertEquals(LocalDateTime.of(2026, 6, 1, 9, 30), readPlain("\"2026-06-01 09:30\""), "分钟精度");
        Assertions.assertEquals(LocalDateTime.of(2026, 6, 1, 0, 0), readPlain("\"2026-06-01\""), "只给日期");
        Assertions.assertEquals(LocalDateTime.of(2026, 6, 1, 9, 30), readPlain("\"2026-06-01T09:30:00\""), "ISO");
    }

    @Test
    void fieldPatternThatDoesNotMatchFallsBackToLenientParsing() {
        // dayOnly 标的是 yyyy-MM-dd，这里喂完整时间：字段级格式解析不了，应回落到通用逻辑，
        // 而不是把原本能通过的请求拒掉 —— 否则给字段加一个只想影响输出的注解就会弄坏入参
        Sample parsed = jsonMapper.readValue("{\"dayOnly\":\"2026-06-01 09:30:00\"}", Sample.class);

        Assertions.assertEquals(LocalDateTime.of(2026, 6, 1, 9, 30), parsed.dayOnly);
    }

    private LocalDateTime readPlain(String jsonValue) {
        return jsonMapper.readValue("{\"plain\":" + jsonValue + "}", Plain.class).plain;
    }
}
