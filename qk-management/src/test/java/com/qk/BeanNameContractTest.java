package com.qk;

import cn.hutool.core.bean.BeanUtil;
import com.qk.entity.dto.ActivitySaveDto;
import com.qk.entity.dto.BusinessSaveDto;
import com.qk.entity.dto.BusinessTrackDto;
import com.qk.entity.dto.ClueSaveDto;
import com.qk.entity.dto.ClueTrackDto;
import com.qk.entity.dto.CourseSaveDto;
import com.qk.entity.dto.CustomerSaveDto;
import com.qk.entity.dto.DeptSaveDto;
import com.qk.entity.dto.RoleSaveDto;
import com.qk.entity.dto.UserSaveDto;
import com.qk.entity.po.Activity;
import com.qk.entity.po.Business;
import com.qk.entity.po.Clue;
import com.qk.entity.po.Course;
import com.qk.entity.po.Customer;
import com.qk.entity.po.Dept;
import com.qk.entity.po.Role;
import com.qk.entity.po.User;
import com.qk.entity.vo.ActivityVO;
import com.qk.entity.vo.CourseVO;
import com.qk.entity.vo.DeptVO;
import com.qk.entity.vo.RoleVO;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 字段名契约守卫
 * <p>
 * DTO / PO / VO 之间的搬运统一用 {@code BeanUtil.copyProperties}，它是<b>按字段名</b>反射拷贝的：
 * 名字对不上不会报错，只会把那个字段留成 null。这条测试就是那个静默风险的唯一防线——
 * 名字一改、少一个字段，这里立刻红，不需要启动 Spring，也不需要连数据库。
 * <p>
 * 两侧都守：<b>DTO → PO</b>（入参不能丢）与 <b>PO → VO</b>（报文不能丢）。
 * PO → VO 只检查「对外字段」：带 {@link JsonIgnore} 的内部列（如逻辑删除位）本来就不出口，不参与比对。
 * <p>
 * 不在覆盖范围内的：Clue / Business / Customer / User 的 VO 由 Mapper XML 的 {@code SELECT c.*}
 * 映射而来（不经过 {@code copyProperties}），那条路径同样是按字段名静默匹配的，但它是否有意省略了
 * 某些列需要逐个确认，因此这里没有纳入；要收编的话得先定清楚每个 VO 该暴露哪些列。
 */
class BeanNameContractTest {

    /** DTO → 实体：DTO 的每个字段都必须在实体里有同名字段 */
    private static final List<Pair> DTO_TO_PO = List.of(
            new Pair(UserSaveDto.class, User.class),
            new Pair(ClueSaveDto.class, Clue.class),
            new Pair(BusinessSaveDto.class, Business.class),
            new Pair(CustomerSaveDto.class, Customer.class),
            new Pair(DeptSaveDto.class, Dept.class),
            new Pair(RoleSaveDto.class, Role.class),
            new Pair(CourseSaveDto.class, Course.class),
            new Pair(ActivitySaveDto.class, Activity.class));

    /**
     * 跟进入参 → 实体：Service 里「跟进」也走 copyProperties。
     * <p>
     * 这两个 DTO 里有几个字段只属于跟进记录（实体上根本没有同名字段），因此显式登记为不搬运，
     * 免得守卫把「本来就不该拷」的字段当成漏拷。
     */
    private static final List<Pair> TRACK_DTO_TO_PO = List.of(
            new Pair(ClueTrackDto.class, Clue.class, Set.of("record")),
            new Pair(BusinessTrackDto.class, Business.class, Set.of("trackStatus", "keyItems", "record")));

    /** 实体 → VO：实体的每个「对外字段」都必须在 VO 里出现 */
    private static final List<Pair> PO_TO_VO = List.of(
            new Pair(Dept.class, DeptVO.class),
            new Pair(Role.class, RoleVO.class),
            new Pair(Course.class, CourseVO.class),
            new Pair(Activity.class, ActivityVO.class));

    @Test
    void everyDtoFieldHasAMatchingEntityField() {
        assertNamesCovered(allCopiedPairs(), "入参字段在实体里找不到同名字段，copyProperties 会静默丢掉它");
    }

    @Test
    void everyExposedEntityFieldAppearsInItsVo() {
        assertNamesCovered(PO_TO_VO, "实体的对外字段在 VO 里找不到同名字段，报文会静默少一列");
    }

    /**
     * 不只查名字，真的跑一遍 {@code copyProperties}
     * <p>
     * 名字对得上但类型不兼容时，Hutool 也可能拷不过去（或抛异常），只有实际复制一次才能发现。
     */
    @Test
    void copyPropertiesReallyCarriesEveryField() {
        for (Pair pair : allCopiedPairs()) {
            assertCopied(pair, mappedFieldsOf(pair));
        }
        for (Pair pair : PO_TO_VO) {
            assertCopied(pair, mappedFieldsOf(pair));
        }
    }

    /** 所有会走 copyProperties 的 DTO → 实体映射 */
    private static List<Pair> allCopiedPairs() {
        List<Pair> pairs = new ArrayList<>(DTO_TO_PO);
        pairs.addAll(TRACK_DTO_TO_PO);
        return pairs;
    }

    private static void assertNamesCovered(List<Pair> pairs, String message) {
        for (Pair pair : pairs) {
            for (Field source : mappedFieldsOf(pair)) {
                Field target = fieldOrNull(pair.target(), source.getName());
                assertNotNull(target, message + "：" + pair.describe(source.getName()));
                assertTrue(boxed(target.getType()).isAssignableFrom(boxed(source.getType())),
                        "两边字段类型不兼容：" + pair.describe(source.getName())
                                + "（" + source.getType().getSimpleName() + " → " + target.getType().getSimpleName() + "）");
            }
        }
    }

    private static void assertCopied(Pair pair, List<Field> expected) {
        Object source = newInstance(pair.source());
        expected.forEach(field -> set(source, field, sampleValue(field.getType())));

        Object target = newInstance(pair.target());
        BeanUtil.copyProperties(source, target);

        for (Field field : expected) {
            Field targetField = fieldOrNull(pair.target(), field.getName());
            assertNotNull(targetField, "目标类没有同名字段：" + pair.describe(field.getName()));
            assertNotNull(read(target, targetField),
                    "copyProperties 没把字段搬过去：" + pair.describe(field.getName()));
        }
    }

    /**
     * 参与搬运的源字段
     * <p>
     * 排除两类：带 {@link JsonIgnore} 的内部列（如逻辑删除位，本来就不出口），
     * 以及映射对里显式登记「不搬运」的字段（如只进跟进记录的 record）。
     */
    private static List<Field> mappedFieldsOf(Pair pair) {
        return fieldsOf(pair.source()).stream()
                .filter(field -> !field.isAnnotationPresent(JsonIgnore.class))
                .filter(field -> !pair.notMapped().contains(field.getName()))
                .toList();
    }

    private static List<Field> fieldsOf(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
                .filter(field -> !field.isSynthetic())
                .toList();
    }

    private static Field fieldOrNull(Class<?> type, String name) {
        return fieldsOf(type).stream().filter(field -> field.getName().equals(name)).findFirst().orElse(null);
    }

    private static Object newInstance(Class<?> type) {
        try {
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("无法实例化 " + type.getSimpleName() + "（缺少无参构造器）", e);
        }
    }

    private static void set(Object target, Field field, Object value) {
        try {
            field.setAccessible(true);
            field.set(target, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("字段赋值失败：" + field.getName(), e);
        }
    }

    private static Object read(Object target, Field field) {
        try {
            field.setAccessible(true);
            return field.get(target);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("字段读取失败：" + field.getName(), e);
        }
    }

    /** 造一个非 null 的样本值；出现未登记的新类型时直接失败，提醒补上样本 */
    private static Object sampleValue(Class<?> type) {
        if (type == String.class) {
            return "sample";
        }
        if (type == Long.class) {
            return 1L;
        }
        if (type == Integer.class) {
            return 1;
        }
        if (type == Boolean.class) {
            return Boolean.TRUE;
        }
        if (type == LocalDateTime.class) {
            return LocalDateTime.of(2026, 6, 1, 9, 0, 0);
        }
        if (type == BigDecimal.class) {
            return new BigDecimal("1.5");
        }
        throw new IllegalStateException("字段类型 " + type.getSimpleName() + " 没有登记样本值，请补进 sampleValue()");
    }

    /** 基本类型装箱成包装类型；本项目 DTO/PO/VO 的字段都是包装类型，这里只是兜底 */
    private static Class<?> boxed(Class<?> type) {
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        return type;
    }

    /**
     * 一对映射
     *
     * @param notMapped 源里存在、但有意不搬运的字段名（在目标上没有同名字段）
     */
    private record Pair(Class<?> source, Class<?> target, Set<String> notMapped) {

        Pair(Class<?> source, Class<?> target) {
            this(source, target, Set.of());
        }

        String describe(String field) {
            return source.getSimpleName() + "." + field + " → " + target.getSimpleName();
        }
    }
}
