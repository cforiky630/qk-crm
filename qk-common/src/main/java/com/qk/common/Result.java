package com.qk.common;

import lombok.Data;

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

    /** 编码：1 成功，0 失败 */
    private Integer code;

    /** 提示信息 */
    private String msg;

    /** 业务数据 */
    private T data;

    /** 成功，且不携带业务数据 */
    public static Result<Void> success() {
        return of(1, "success", null);
    }

    /** 成功，并携带业务数据 */
    public static <T> Result<T> success(T data) {
        return of(1, "success", data);
    }

    /**
     * 失败
     * <p>
     * 方法本身是泛型的，因此 {@code return x != null ? Result.success(x) : Result.error("不存在");}
     * 这类写法可以继续编译：目标类型 {@code Result<X>} 会同时作用于两个分支。
     */
    public static <T> Result<T> error(String msg) {
        return of(0, msg, null);
    }

    private static <T> Result<T> of(Integer code, String msg, T data) {
        Result<T> result = new Result<>();
        result.code = code;
        result.msg = msg;
        result.data = data;
        return result;
    }

}
