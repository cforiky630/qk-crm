package com.qk;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qk.common.util.JwtUtil;
import com.qk.entity.enums.BusinessStatus;
import com.qk.entity.enums.ClueStatus;
import com.qk.entity.po.Business;
import com.qk.entity.po.Clue;
import com.qk.entity.po.Customer;
import com.qk.entity.po.Role;
import com.qk.entity.po.User;
import com.qk.mapper.BusinessMapper;
import com.qk.mapper.ClueMapper;
import com.qk.mapper.CustomerMapper;
import com.qk.mapper.RoleMapper;
import com.qk.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 数据隔离的接口级验证（走真实 SQL）
 * <p>
 * 守住的核心承诺：<b>专员看不见别人名下的数据，但看不见「还没有归属人」的数据不算隔离</b>——
 * 后者必须仍然可见，否则会出现「刚新增、还没分配的东西自己就看不见了」。
 * 客户表没有归属人字段，归属人来自它的来源商机，因此这里是「商机专员A看不见被分配给B的客户」的直接验证。
 */
@SpringBootTest
@Transactional
class DataIsolationTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private BusinessMapper businessMapper;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private ClueMapper clueMapper;

    @Autowired
    private JwtUtil jwtUtil;

    private MockMvc mockMvc;
    private Long adminId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        adminId = userWithBuiltinRole("admin");
    }

    /** 商机专员A 看不见「由分配给B的商机」转化来的客户 */
    @Test
    void operatorCannotSeeCustomerOwnedByAnotherOperator() throws Exception {
        Long ownerB = userWithBuiltinRole("business_operator");
        Long operatorA = userWithBuiltinRole("business_operator");
        Customer customer = insertCustomerOwnedBy(ownerB);

        // A：列表里搜不到，详情表现为「不存在」（不泄露该客户是否存在）
        mockMvc.perform(get("/customers").header("token", tokenFor(operatorA)).param("phone", customer.getPhone()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.total").value(0));
        mockMvc.perform(get("/customers/{id}", customer.getId()).header("token", tokenFor(operatorA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("客户不存在"));

        // B 本人与管理员都看得见
        mockMvc.perform(get("/customers/{id}", customer.getId()).header("token", tokenFor(ownerB)))
                .andExpect(jsonPath("$.code").value(1));
        mockMvc.perform(get("/customers/{id}", customer.getId()).header("token", tokenFor(adminId)))
                .andExpect(jsonPath("$.code").value(1));
    }

    /** 还没有归属人的客户（手工新增、没有来源商机）对专员仍然可见 */
    @Test
    void customerWithoutSourceBusinessStaysVisible() throws Exception {
        Long operator = userWithBuiltinRole("business_operator");
        Customer customer = insertCustomer(null);

        mockMvc.perform(get("/customers/{id}", customer.getId()).header("token", tokenFor(operator)))
                .andExpect(jsonPath("$.code").value(1));
    }

    /** 线索专员看不见分配给别人的线索 */
    @Test
    void operatorCannotSeeClueAssignedToAnotherOperator() throws Exception {
        Long ownerB = userWithBuiltinRole("clue_operator");
        Long operatorA = userWithBuiltinRole("clue_operator");
        Clue clue = insertClue(ownerB);

        mockMvc.perform(get("/clues/{id}", clue.getId()).header("token", tokenFor(operatorA)))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("线索不存在"));
        mockMvc.perform(get("/clues/{id}", clue.getId()).header("token", tokenFor(ownerB)))
                .andExpect(jsonPath("$.code").value(1));
        mockMvc.perform(get("/clues/{id}", clue.getId()).header("token", tokenFor(adminId)))
                .andExpect(jsonPath("$.code").value(1));
    }

    /** 尚未分配的线索对专员可见（未分配不等于「别人的」） */
    @Test
    void unassignedClueStaysVisible() throws Exception {
        Long operator = userWithBuiltinRole("clue_operator");
        Clue clue = insertClue(null);

        mockMvc.perform(get("/clues/{id}", clue.getId()).header("token", tokenFor(operator)))
                .andExpect(jsonPath("$.code").value(1));
    }

    private String tokenFor(Long userId) {
        return jwtUtil.generateToken(Map.of("id", userId, "username", "data_ut_" + userId));
    }

    private Long userWithBuiltinRole(String roleLabel) {
        Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getLabel, roleLabel));
        assertNotNull(role, "种子数据缺少内置角色: " + roleLabel);
        return insertUser(role.getId());
    }

    private Long insertUser(Long roleId) {
        int seq = SEQ.incrementAndGet();
        User user = new User();
        user.setUsername("data_user_" + seq);
        user.setName("数据隔离测试" + seq);
        user.setPhone("155" + String.format("%08d", seq));
        user.setEmail("data_user_" + seq + "@qk.test");
        user.setPassword(DigestUtil.md5Hex(user.getUsername() + "123"));
        user.setGender(1);
        user.setStatus(1);
        user.setDeptId(1L);
        user.setRoleId(roleId);
        userMapper.insert(user);
        return user.getId();
    }

    /** 造一条「归属人为 ownerId 的商机」，再把它转成客户，返回客户 */
    private Customer insertCustomerOwnedBy(Long ownerId) {
        Business business = new Business();
        business.setName("隔离测试商机");
        business.setPhone(phone());
        business.setStatus(BusinessStatus.CONVERT_CUSTOMER.getCode());
        business.setUserId(ownerId);
        businessMapper.insert(business);
        return insertCustomer(business.getId());
    }

    /** businessId 为 null 表示手工新增的客户（没有来源商机，也就没有归属人） */
    private Customer insertCustomer(Long businessId) {
        Customer customer = new Customer();
        customer.setName("隔离测试客户");
        customer.setPhone(phone());
        customer.setBusinessId(businessId);
        customerMapper.insert(customer);
        return customer;
    }

    private Clue insertClue(Long ownerId) {
        Clue clue = new Clue();
        clue.setName("隔离测试线索");
        clue.setPhone(phone());
        clue.setChannel(1);
        clue.setStatus(ClueStatus.WAIT_FOLLOW.getCode());
        clue.setUserId(ownerId);
        clueMapper.insert(clue);
        return clue;
    }

    private static String phone() {
        return "133" + String.format("%08d", SEQ.incrementAndGet());
    }
}
