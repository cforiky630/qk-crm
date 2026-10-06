package com.qk.config;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;

/**
 * JSON 日期时间格式配置
 * <p>
 * 输出默认固定为 {@code yyyy-MM-dd HH:mm:ss}；输入额外兼容 {@code yyyy-MM-dd HH:mm}、
 * {@code yyyy-MM-dd}（按当天 00:00:00 处理）与 ISO 格式 {@code yyyy-MM-ddTHH:mm:ss}，
 * 降低前后端联调成本。
 * <p>
 * 个别字段需要别的格式时，在该字段上写 {@code @JsonFormat(pattern = "...")} 即可覆盖默认；
 * 入参同理，只是字段级格式解析不出来时仍会回落到上面的兼容逻辑 ——
 * 免得一个原本只想影响输出的注解，把入参也弄坏。
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
     * 取字段上的 {@code @JsonFormat(pattern = ...)}
     *
     * @return 字段级格式；没写、或写了空串时返回 null，表示沿用全局默认
     */
    private static DateTimeFormatter fieldPattern(BeanProperty property) {
        if (property == null) {
            return null;
        }
        JsonFormat format = property.getAnnotation(JsonFormat.class);
        if (format == null || format.pattern() == null || format.pattern().isEmpty()) {
            return null;
        }
        return DateTimeFormatter.ofPattern(format.pattern());
    }

    /**
     * 默认按 {@code yyyy-MM-dd HH:mm:ss} 输出；字段上写了 {@code @JsonFormat} 时改用字段的格式
     */
    static class QkLocalDateTimeSerializer extends ValueSerializer<LocalDateTime> {

        private final DateTimeFormatter formatter;

        QkLocalDateTimeSerializer() {
            this(DATE_TIME);
        }

        private QkLocalDateTimeSerializer(DateTimeFormatter formatter) {
            this.formatter = formatter;
        }

        @Override
        public ValueSerializer<?> createContextual(SerializationContext ctxt, BeanProperty property) {
            DateTimeFormatter custom = fieldPattern(property);
            return custom == null ? this : new QkLocalDateTimeSerializer(custom);
        }

        @Override
        public void serialize(LocalDateTime value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
            gen.writeString(formatter.format(value));
        }
    }

    /**
     * 兼容多种常见的时间字符串写法；字段上写了 {@code @JsonFormat} 时优先按字段的格式解析
     */
    static class QkLocalDateTimeDeserializer extends ValueDeserializer<LocalDateTime> {

        /** 字段级格式；为 null 表示没配，直接走下面的通用兼容逻辑 */
        private final DateTimeFormatter fieldFormatter;

        QkLocalDateTimeDeserializer() {
            this(null);
        }

        private QkLocalDateTimeDeserializer(DateTimeFormatter fieldFormatter) {
            this.fieldFormatter = fieldFormatter;
        }

        @Override
        public ValueDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
            DateTimeFormatter custom = fieldPattern(property);
            return custom == null ? this : new QkLocalDateTimeDeserializer(custom);
        }

        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
            String text = p.getString();
            if (text == null || text.isBlank()) {
                return null;
            }
            String value = text.trim();

            LocalDateTime byFieldPattern = parseByFieldPattern(value);
            if (byFieldPattern != null) {
                return byFieldPattern;
            }

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

        /**
         * 按字段级 {@code @JsonFormat} 解析
         * <p>
         * 只给日期的格式（如 {@code yyyy-MM-dd}）同样按当天零点处理，与通用逻辑保持一致。
         *
         * @return 字段级格式解析出的时间；没配该格式、或这个字符串不符合该格式时返回 null，交给通用逻辑
         */
        private LocalDateTime parseByFieldPattern(String value) {
            if (fieldFormatter == null) {
                return null;
            }
            try {
                TemporalAccessor parsed = fieldFormatter.parse(value);
                return parsed.isSupported(ChronoField.HOUR_OF_DAY)
                        ? LocalDateTime.from(parsed)
                        : LocalDate.from(parsed).atStartOfDay();
            } catch (DateTimeException e) {
                return null;
            }
        }
    }
}
