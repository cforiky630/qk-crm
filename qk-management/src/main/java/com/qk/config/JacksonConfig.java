package com.qk.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * JSON 日期时间格式配置
 * <p>
 * Spring Boot 4 使用的是 Jackson 3（tools.jackson.*），java.time 的支持已内置在 jackson-databind 中，
 * 但默认只认 ISO 格式（2025-06-01T00:00:00）。接口文档中前端传递与展示的都是
 * 「yyyy-MM-dd HH:mm:ss」格式，因此这里统一注册自定义的序列化/反序列化器：
 * <ul>
 *   <li>输出：统一格式化为 yyyy-MM-dd HH:mm:ss</li>
 *   <li>输入：同时兼容 yyyy-MM-dd HH:mm:ss、yyyy-MM-dd HH:mm、yyyy-MM-dd 与 ISO 格式，降低前后端联调成本</li>
 * </ul>
 */
@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_TIME_MINUTE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Bean
    public JsonMapperBuilderCustomizer qkLocalDateTimeCustomizer() {
        return builder -> builder.addModule(new SimpleModule("qkLocalDateTimeModule")
                .addSerializer(LocalDateTime.class, new QkLocalDateTimeSerializer())
                .addDeserializer(LocalDateTime.class, new QkLocalDateTimeDeserializer()));
    }

    /**
     * 统一按 yyyy-MM-dd HH:mm:ss 输出
     */
    static class QkLocalDateTimeSerializer extends ValueSerializer<LocalDateTime> {

        @Override
        public void serialize(LocalDateTime value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
            gen.writeString(DATE_TIME.format(value));
        }
    }

    /**
     * 兼容多种常见的时间字符串写法
     */
    static class QkLocalDateTimeDeserializer extends ValueDeserializer<LocalDateTime> {

        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
            String text = p.getString();
            if (text == null || text.isBlank()) {
                return null;
            }
            String value = text.trim();

            // ISO 格式：2025-06-01T00:00:00
            if (value.contains("T")) {
                return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
            // 只有日期：2025-06-01，按当天零点处理
            if (value.length() <= 10) {
                return LocalDate.parse(value).atStartOfDay();
            }
            // yyyy-MM-dd HH:mm:ss
            try {
                return LocalDateTime.parse(value, DATE_TIME);
            } catch (DateTimeParseException e) {
                // yyyy-MM-dd HH:mm
                return LocalDateTime.parse(value, DATE_TIME_MINUTE);
            }
        }
    }
}
