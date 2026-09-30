package com.qk.common;

import lombok.Data;

import java.util.Objects;

/**
 * 后端统一返回结果
 * <p>
 * 泛型参数 {@code T} 是业务数据的类型，例如 {@code Result<UserVO>}、
 * {@code Result<PageResult<ClueVO>>}。这样接口的返回类型自解释，
 * 调用方不需要再对 {@code data} 做强制类型转换。
 * <p>
 * 注意：泛型只作用于编译期，JSON 输出与原先完全一致
 * （字段仍是 {@code code}、{@code msg}、{@code data}）。
 *
 * @param <T> 业务数据类型；无数据时使用 {@link Void}
 */
@Data
public class Result<T> {

    /** 编码，取值见 {@link ResultCode}：1 成功，0 失败 */
    private Integer code;

    /** 提示信息 */
    private String msg;

    /** 业务数据 */
    private T data;

    /**
     * 私有构造：统一由静态工厂创建
     * <p>
     * 若保留隐式公开构造，外部就能 {@code new Result<>()} 造出 code / msg 都为 null 的非法响应，
     * 绕过 {@link ResultCode} 的约束。私有化之后，响应只能来自
     * {@link #success()}、{@link #success(Object)}、{@link #error(String)}、{@link #custom(ResultCode, String, Object)}。
     */
    private Result() {
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
        Result<T> result = new Result<>();
        result.code = resultCode.getCode();
        result.msg = msg;
        result.data = data;
        return result;
    }

    private static <T> Result<T> of(ResultCode resultCode, T data) {
        return custom(resultCode, resultCode.getMsg(), data);
    }

}
