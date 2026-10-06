package com.qk;

import com.qk.entity.po.Activity;
import com.qk.entity.po.Course;
import com.qk.entity.po.Dept;
import com.qk.entity.po.Role;
import com.qk.entity.vo.ActivityVO;
import com.qk.entity.vo.CourseVO;
import com.qk.entity.vo.DeptVO;
import com.qk.entity.vo.RoleVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 出参模型测试：视图对象与持久化对象的对外报文必须逐字节一致
 * <p>
 * 部门/角色/课程/活动这四张表改成「PO 不出口」之后，报文由 VO 生成。
 * 这条测试把同一个 PO 分别以自身和 VO 的形式序列化再比对，
 * 用来保证改造前后字段名、字段数量、字段类型完全不变——VO 少抄一个字段就会在这里失败。
 * <p>
 * 这里用新建的 {@code JsonMapper} 而不是容器里的那一个：比较的是两侧在同一序列化器下的结果，
 * 与具体配置无关，因此不需要启动 Spring 上下文。
 */
class OutputModelTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void deptVoSerializesExactlyLikeDept() {
        Dept po = new Dept();
        po.setId(7L);
        po.setName("市场部");
        po.setStatus(1);
        po.setCreateTime(LocalDateTime.of(2026, 6, 1, 9, 0, 0));
        po.setUpdateTime(LocalDateTime.of(2026, 6, 2, 10, 30, 0));
        po.setDeleted(Boolean.TRUE);
        assertSameWireFormat(po, DeptVO.from(po));
    }

    @Test
    void roleVoSerializesExactlyLikeRole() {
        Role po = new Role();
        po.setId(1L);
        po.setName("管理员");
        po.setLabel("admin");
        po.setRemark("拥有全部权限");
        po.setCreateTime(LocalDateTime.of(2026, 6, 1, 9, 0, 0));
        po.setUpdateTime(LocalDateTime.of(2026, 6, 2, 10, 30, 0));
        po.setDeleted(Boolean.TRUE);
        assertSameWireFormat(po, RoleVO.from(po));
    }

    @Test
    void courseVoSerializesExactlyLikeCourse() {
        Course po = new Course();
        po.setId(1L);
        po.setSubject(2);
        po.setName("Python大模型应用开发");
        po.setPrice(899);
        po.setTarget(2);
        po.setDescription("有编程基础，进阶大模型应用开发");
        po.setCreateTime(LocalDateTime.of(2026, 6, 1, 9, 0, 0));
        po.setUpdateTime(LocalDateTime.of(2026, 6, 2, 10, 30, 0));
        po.setDeleted(Boolean.TRUE);
        assertSameWireFormat(po, CourseVO.from(po));
    }

    @Test
    void activityVoSerializesExactlyLikeActivity() {
        Activity po = new Activity();
        po.setId(1L);
        po.setChannel(1);
        po.setName("618 Java课程折扣");
        po.setStartTime(LocalDateTime.of(2026, 6, 1, 0, 0, 0));
        po.setEndTime(LocalDateTime.of(2026, 6, 30, 23, 59, 59));
        po.setDescription("Java 课程限时 8 折");
        po.setType(1);
        po.setDiscount(new BigDecimal("8.0"));
        po.setVoucher(null);
        po.setCreateTime(LocalDateTime.of(2026, 6, 1, 9, 0, 0));
        po.setUpdateTime(LocalDateTime.of(2026, 6, 2, 10, 30, 0));
        po.setDeleted(Boolean.TRUE);
        assertSameWireFormat(po, ActivityVO.from(po));
    }

    /**
     * 同一个持久化对象，直接序列化（改造前的报文形态）与经 VO 序列化（改造后）必须完全一致。
     * 同时确认逻辑删除列不会随实体泄漏出去。
     */
    private void assertSameWireFormat(Object po, Object vo) {
        String poJson = jsonMapper.writeValueAsString(po);
        String voJson = jsonMapper.writeValueAsString(vo);

        Assertions.assertEquals(poJson, voJson,
                "视图对象改变了对外报文：" + po.getClass().getSimpleName());
        Assertions.assertFalse(poJson.contains("deleted"), "报文里不得出现逻辑删除列: " + poJson);
        Assertions.assertFalse(poJson.contains("is_deleted"), "报文里不得出现逻辑删除列: " + poJson);
    }
}
