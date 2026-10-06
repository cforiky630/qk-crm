package com.qk.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;

/**
 * 后端统一返回结果
 * <p>
 * 泛型参数 {@code T} 是业务数据的类型，例如 {@code Result<UserVO>}、
 * {@code Result<PageResult<ClueVO>>}。这样接口的返回类型自解释，
 * 调用方不需要再对 {@code data} 做强制类型转换。
 * <p>
 * 注意：泛型只作用于编译期，JSON 结构固定为 {@code code}、{@code msg}、{@code data}
 * 三个字段；类上标注 {@code @JsonInclude(NON_NULL)}，因此 {@code data} 为 {@code null}
 * 时该字段不会出现在报文里（调用方判断成功请用 {@code code == 1}）。
 * <p>
 * 本类<b>不可变</b>：字段全部 final、构造器私有、不提供 setter。
 * 实例只能由 {@link #success()}、{@link #success(Object)}、{@link #error(String)}、
 * {@link #custom(ResultCode, String, Object)} 创建，从根本上杜绝
 * 「响应码为空」与「创建后被篡改」这两类非法响应。
 *
 * @param <T> 业务数据类型；无数据时使用 {@link Void}
 */
@Getter
@ToString
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> {

    /** 编码，取值见 {@link ResultCode}：1 成功，0 失败 */
    private final Integer code;

    /** 提示信息 */
    private final String msg;

    /** 业务数据 */
    private final T data;

    /** 私有构造：只能由下方的静态工厂调用，外部无法 new 出响应 */
    private Result(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /** 成功，且不携带业务数据 */
    public static Result<Void> success() {
        return of(ResultCode.SUCCESS, null);
    }

    /** 成功，并携带业务数据 */
    public static <T> Result<T> success(T data) {
        return of(ResultCode.SUCCESS, data);
    }

    /**
     * 失败
     * <p>
     * 方法本身是泛型的，因此 {@code return x != null ? Result.success(x) : Result.error("不存在");}
     * 这类写法可以继续编译：目标类型 {@code Result<X>} 会同时作用于两个分支。
     */
    public static <T> Result<T> error(String msg) {
        return custom(ResultCode.FAIL, msg, null);
    }

    /**
     * 自定义响应：自行指定响应码与提示语，用于极少数 {@link #success()} / {@link #error(String)}
     * 覆盖不了的场景。
     * <p>
     * <b>约定：能不用自定义就不用自定义。</b>
     * <ul>
     *   <li>常规的成功与失败一律使用 {@link #success()}、{@link #success(Object)}、{@link #error(String)}，
     *       它们把响应码固定为 {@code 1} / {@code 0}，这是对外契约的一部分，不可更改；</li>
     *   <li>只有「同一响应码需要换一句提示语」或「确需新增响应码」时才用本方法；</li>
     *   <li>确需新增响应码时，必须先在 {@link ResultCode} 中增加带注释的枚举成员，
     *       并同步更新 docs/openapi.yaml 的状态码约定，不允许在调用处临时拼一个码值；</li>
     *   <li>响应码只能取自 {@link ResultCode}，不接受裸数字，避免重新引入魔法值。</li>
     * </ul>
     *
     * @param resultCode 响应码，取值见 {@link ResultCode}
     * @param msg        提示信息，可直接展示给用户
     * @param data       业务数据，无数据时传 null
     * @return 组装好的响应对象
     */
    public static <T> Result<T> custom(ResultCode resultCode, String msg, T data) {
        Objects.requireNonNull(resultCode, "响应码不能为空，取值见 ResultCode");
        return new Result<>(resultCode.getCode(), msg, data);
    }

    private static <T> Result<T> of(ResultCode resultCode, T data) {
        return custom(resultCode, resultCode.getMsg(), data);
    }

}
