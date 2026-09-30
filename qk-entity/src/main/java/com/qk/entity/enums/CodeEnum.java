package com.qk.entity.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 码值枚举统一契约
 * <p>
 * 凡是「对外用数字编码、对内用枚举表达」的枚举都实现本接口，例如
 * {@link ClueStatus}、{@link BusinessStatus}、{@link ClueTrackType}。
 * 相比各自声明 code 字段和 getter，这里统一了两件事：
 * <ol>
 *   <li>取值：一律用 {@link #getCode()}；</li>
 *   <li>反查：用 {@link #fromCode(Class, Serializable)} 按码值找回枚举，避免各处重复写循环。</li>
 * </ol>
 * 本接口同时继承 MyBatis-Plus 的 {@link IEnum}，因此当前只当普通枚举用，
 * 将来若把实体字段直接声明为枚举类型，MyBatis-Plus 会自动完成数据库值与枚举的互转，
 * 不需要再改动枚举本身。
 * <p>
 * 约定：实现类必须保证码值在同一个枚举内唯一（{@link #fromCode} 只返回第一个匹配项）。
 *
 * @param <T> 码值类型，例如 Integer
 */
public interface CodeEnum<T extends Serializable> extends IEnum<T> {

    /**
     * 码值
     * <p>
     * 默认复用 MyBatis-Plus 约定的 {@link #getValue()}：
     * 实现类只要声明一个 value 字段（Lombok 的 {@code @Getter} 即可满足），
     * 调用方依旧使用语义更明确的 getCode()。
     */
    default T getCode() {
        return getValue();
    }

    /**
     * 按码值反查枚举
     *
     * @param type 枚举类型，例如 {@code ClueStatus.class}
     * @param code 码值
     * @return 匹配到的枚举；类型为空、码值为 null 或码值不存在时返回 {@link Optional#empty()}
     */
    static <T extends Serializable, E extends Enum<E> & CodeEnum<T>> Optional<E> fromCode(Class<E> type, T code) {
        if (type == null || code == null) {
            return Optional.empty();
        }
        for (E constant : type.getEnumConstants()) {
            if (code.equals(constant.getValue())) {
                return Optional.of(constant);
            }
        }
        return Optional.empty();
    }

    /**
     * 枚举的全部码值，顺序与枚举声明顺序一致。
     * 适合做参数校验，或给前端生成下拉选项。
     */
    static <T extends Serializable, E extends Enum<E> & CodeEnum<T>> List<T> codes(Class<E> type) {
        List<T> codes = new ArrayList<>();
        if (type == null) {
            return codes;
        }
        for (E constant : type.getEnumConstants()) {
            codes.add(constant.getValue());
        }
        return codes;
    }
}
