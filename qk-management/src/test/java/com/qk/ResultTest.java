package com.qk;

import com.qk.common.Result;
import com.qk.common.ResultCode;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定 Result 的对外契约
 * <p>
 * 泛型化只影响编译期，接口的 JSON 结构必须与改造前完全一致：
 * 固定三个字段 code / msg / data，且 data 为 null 时字段依然存在（不会被省略）。
 */
class ResultTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    /**
     * 响应码是对外契约，取值被永久冻结：1 成功、0 失败。
     * 这个断言的作用是防止以后有人"顺手"改成别的编码体系。
     */
    @Test
    void resultCodeValuesAreFrozen() {
        assertEquals(1, ResultCode.SUCCESS.getCode());
        assertEquals("success", ResultCode.SUCCESS.getMsg());
        assertEquals(0, ResultCode.FAIL.getCode());
    }

    @Test
    void successWithoutDataKeepsJsonShape() {
        Result<Void> result = Result.success();

        assertEquals(1, result.getCode());
        assertEquals("success", result.getMsg());
        assertNull(result.getData());

        Map<String, Object> json = toMap(result);
        assertEquals(3, json.size(), "响应体必须固定为 code/msg/data 三个字段");
        assertEquals(1, ((Number) json.get("code")).intValue());
        assertEquals("success", json.get("msg"));
        assertTrue(json.containsKey("data"), "data 为 null 时字段也必须存在");
        assertNull(json.get("data"));
    }

    @Test
    void successWithDataCarriesPayload() {
        Result<String> result = Result.success("hello");

        assertEquals(1, result.getCode());
        assertEquals("hello", result.getData());
        assertEquals("hello", toMap(result).get("data"));
    }

    @Test
    void errorCarriesMessageAndZeroCode() {
        Result<String> result = Result.error("客户不存在");

        assertEquals(0, result.getCode());
        assertEquals("客户不存在", result.getMsg());
        assertNull(result.getData());
        assertEquals(0, ((Number) toMap(result).get("code")).intValue());
    }

    /**
     * 自定义入口是逃生舱：允许换提示语，但响应码仍必须来自 ResultCode，
     * 不允许在调用处拼裸数字。
     */
    @Test
    void customCarriesChosenCodeAndMessage() {
        Result<String> result = Result.custom(ResultCode.FAIL, "手机号已存在", "payload");

        assertEquals(0, result.getCode());
        assertEquals("手机号已存在", result.getMsg());
        assertEquals("payload", result.getData());
        assertEquals("手机号已存在", toMap(result).get("msg"));
    }

    @Test
    void customRejectsNullResultCode() {
        NullPointerException error = assertThrows(NullPointerException.class,
                () -> Result.custom(null, "提示", null));

        assertTrue(error.getMessage().contains("ResultCode"), "异常信息应指向 ResultCode");
    }

    /**
     * 三元表达式里同时出现 success 与 error。
     * error 声明为泛型方法后，两个分支才能统一到目标类型 Result&lt;String&gt;。
     */
    @Test
    void ternaryOfSuccessAndErrorKeepsType() {
        String value = null;
        Result<String> result = value != null ? Result.success(value) : Result.error("不存在");

        assertEquals(0, result.getCode());
        assertEquals("不存在", result.getMsg());
        assertNull(result.getData());
    }

    private Map<String, Object> toMap(Result<?> result) {
        return jsonMapper.readValue(jsonMapper.writeValueAsString(result),
                new TypeReference<Map<String, Object>>() {
                });
    }
}
