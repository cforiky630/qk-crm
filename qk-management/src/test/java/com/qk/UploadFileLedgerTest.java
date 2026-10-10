package com.qk;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.qk.common.util.JwtUtil;
import com.qk.entity.enums.UploadStatus;
import com.qk.entity.po.UploadFile;
import com.qk.entity.po.User;
import com.qk.mapper.UploadFileMapper;
import com.qk.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 上传文件台账与业务绑定的集成测试
 * <p>
 * 覆盖两件只有连库才能验证的事：
 * <ol>
 *   <li>新增用户时会把头像对应的台账记录标记为「已绑定」并写上业务主键；</li>
 *   <li>引用扫描必须排除逻辑删除的用户，否则删掉的用户会永远占着头像、回收不掉。</li>
 * </ol>
 */
@SpringBootTest
@Transactional
class UploadFileLedgerTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UploadFileMapper uploadFileMapper;

    @Autowired
    private JwtUtil jwtUtil;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        String token = jwtUtil.generateToken(Map.of("id", 1, "username", "zhangsan"));
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .defaultRequest(get("/").header("token", token))
                .build();
    }

    @Test
    void creatingUserBindsUploadedAvatarToUser() throws Exception {
        UploadFile uploaded = insertUploadedFile();
        String username = "ledger" + SEQ.incrementAndGet();

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","name":"绑定测试","phone":"%s","email":"%s@qk.test","gender":1,"status":1,"image":"%s"}
                                """.formatted(username, phone(), username, uploaded.getUrl())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        User created = userMapper.findByUsername(username);
        assertNotNull(created, "用户应已创建");

        UploadFile bound = uploadFileMapper.selectById(uploaded.getId());
        assertEquals(UploadStatus.BOUND.getCode(), bound.getStatus(), "新增用户后头像台账应标记为已绑定");
        assertEquals("user", bound.getRefType());
        assertEquals(created.getId(), bound.getRefId());
        assertNotNull(bound.getBindTime());
    }

    @Test
    void referenceScanExcludesLogicallyDeletedUsers() {
        String liveUrl = "https://example.test/live-" + SEQ.incrementAndGet() + ".png";
        String deletedUrl = "https://example.test/deleted-" + SEQ.incrementAndGet() + ".png";
        Long liveId = insertUser(liveUrl);
        Long deletedId = insertUser(deletedUrl);

        userMapper.deleteByIds(List.of(deletedId));

        List<String> referenced = userMapper.listImageUrls();
        assertTrue(referenced.contains(liveUrl), "未删除用户的头像必须仍被算作引用");
        assertTrue(referenced.stream().noneMatch(deletedUrl::equals),
                "逻辑删除的用户不能再算作头像引用，否则旧头像永远回收不掉");
        assertNotNull(liveId);
    }

    /**
     * 台账状态流转的真实 SQL：抢占回收权是条件更新，必须幂等（第二次抢占返回 0），
     * 重试计数随抢占累加，回退能把状态退回「临时」留给下一轮。
     */
    @Test
    void ledgerStateTransitionsWorkAgainstRealSql() {
        UploadFile file = insertUploadedFile();

        assertEquals(1, uploadFileMapper.claimForRecycle(file.getId()), "首次抢占应成功");
        UploadFile claimed = uploadFileMapper.selectById(file.getId());
        assertEquals(UploadStatus.RECYCLED.getCode(), claimed.getStatus());
        assertEquals(1, claimed.getRetryCount(), "抢占要累计回收尝试次数");

        assertEquals(0, uploadFileMapper.claimForRecycle(file.getId()), "已回收的对象不能再被抢占");

        assertEquals(1, uploadFileMapper.revertRecycle(file.getId()));
        assertEquals(UploadStatus.TEMP.getCode(), uploadFileMapper.selectById(file.getId()).getStatus(),
                "删除失败回退后应回到「临时」，否则下一轮不会重试");
    }

    /** 宽限期筛选：只有上传时间早于截止点的对象才进入候选 */
    @Test
    void listRecyclableRespectsGracePeriod() {
        UploadFile fresh = insertUploadedFile();
        UploadFile stale = insertUploadedFile();
        uploadFileMapper.update(null, new LambdaUpdateWrapper<UploadFile>()
                .eq(UploadFile::getId, stale.getId())
                .set(UploadFile::getCreateTime, LocalDateTime.now().minusDays(3)));

        List<UploadFile> candidates = uploadFileMapper.listRecyclable(LocalDateTime.now().minusHours(24), 200);

        assertTrue(candidates.stream().anyMatch(f -> f.getId().equals(stale.getId())),
                "超过宽限期的对象应进入候选");
        assertTrue(candidates.stream().noneMatch(f -> f.getId().equals(fresh.getId())),
                "仍在宽限期内的对象不能被回收");
    }

    /** 模拟一次 POST /upload 之后的台账状态（不真正连 OSS） */
    private UploadFile insertUploadedFile() {
        UploadFile file = new UploadFile();
        String suffix = ".png";
        String md5 = DigestUtil.md5Hex("ledger-" + SEQ.incrementAndGet());
        file.setObjectKey("images/1/" + md5 + suffix);
        file.setUrl("https://example.test/" + md5 + suffix);
        file.setUploaderId(1L);
        file.setContentMd5(md5);
        file.setSize(8);
        file.setContentType("image/png");
        file.setStatus(UploadStatus.TEMP.getCode());
        file.setRetryCount(0);
        uploadFileMapper.insert(file);
        return file;
    }

    private Long insertUser(String image) {
        String username = "ref" + SEQ.incrementAndGet();
        User user = new User();
        user.setUsername(username);
        user.setName("引用测试");
        user.setPhone(phone());
        user.setEmail(username + "@qk.test");
        user.setPassword(DigestUtil.md5Hex(username + "123"));
        user.setGender(1);
        user.setStatus(1);
        user.setImage(image);
        userMapper.insert(user);
        return user.getId();
    }

    private static String phone() {
        return "188" + String.format("%08d", SEQ.incrementAndGet());
    }
}
