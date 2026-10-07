package com.qk.service.impl;

import com.qk.common.exception.BusinessException;
import com.qk.common.context.CurrentUser;
import com.qk.common.storage.FileStorage;
import com.qk.common.util.UserHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 上传策略单测
 * <p>
 * 不连数据库、不连 OSS：{@link FileStorage} 是端口，测试用一个内存实现替掉真实对象存储，
 * 因此校验规则（白名单、文件头、空内容）可以独立、快速地验证 —— 这也是把上传策略
 * 从控制器搬进服务层、把存储抽成端口的直接收益。
 */
class UploadServiceImplTest {

    /** PNG 文件头，够魔术字节校验用 */
    private static final byte[] PNG_HEAD = {
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D};

    private final RecordingStorage storage = new RecordingStorage();
    private final UploadServiceImpl uploadService = new UploadServiceImpl(storage);

    @AfterEach
    void clearUserContext() {
        UserHolder.removeCurrentUser();
    }

    @Test
    void storesValidImageAndReturnsUrl() {
        // 上传接口只需要"当前用户ID"，权限不影响这里；构造一个最小上下文即可
        UserHolder.setCurrentUser(new CurrentUser(42L, null, Set.of()));

        String url = uploadService.upload("photo.png", stream(PNG_HEAD));

        assertEquals("https://example.test/photo.png", url);
        assertEquals(42L, storage.ownerId, "上传人必须透传给存储实现");
        assertEquals("photo.png", storage.filename);
        assertEquals(PNG_HEAD.length, storage.size);
    }

    @Test
    void extensionIsCaseInsensitive() {
        uploadService.upload("PHOTO.PNG", stream(PNG_HEAD));

        assertEquals(1, storage.calls);
    }

    @Test
    void rejectsUnsupportedExtensionBeforeReadingContent() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> uploadService.upload("evil.sh", stream("echo hi".getBytes(StandardCharsets.UTF_8))));

        assertEquals("只支持 jpg、jpeg、png、gif、bmp、webp 格式的图片", e.getMessage());
        assertEquals(0, storage.calls, "校验不通过时不能触碰存储");
    }

    @Test
    void rejectsFileWithoutExtension() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> uploadService.upload("noextension", stream("x".getBytes(StandardCharsets.UTF_8))));

        assertEquals("只支持 jpg、jpeg、png、gif、bmp、webp 格式的图片", e.getMessage());
        assertEquals(0, storage.calls);
    }

    @Test
    void rejectsFileWhoseContentDoesNotMatchItsExtension() {
        // 把脚本改名成 .png：只校验扩展名时会通过，因此必须看文件头
        BusinessException e = assertThrows(BusinessException.class,
                () -> uploadService.upload("evil.png", stream("echo hi".getBytes(StandardCharsets.UTF_8))));

        assertEquals("图片内容与文件类型不匹配，请上传真实图片", e.getMessage());
        assertEquals(0, storage.calls, "文件头不匹配时不能把内容写进对象存储");
    }

    @Test
    void rejectsEmptyContent() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> uploadService.upload("empty.png", stream(new byte[0])));

        assertEquals("请选择要上传的图片", e.getMessage());
        assertEquals(0, storage.calls);
    }

    /**
     * 读取上传文件失败时必须保留根因
     * <p>
     * 对外提示不能变（前端只看得到「读取上传文件失败」），但日志里要能看到到底为什么失败，
     * 否则超时、断流、被拒绝这些情况在排查时全都长一个样。
     */
    @Test
    void uploadKeepsRootCauseWhenReadingFails() {
        IOException rootCause = new IOException("simulated read failure");
        InputStream broken = new InputStream() {
            @Override
            public int read() throws IOException {
                throw rootCause;
            }
        };

        BusinessException thrown = assertThrows(BusinessException.class,
                () -> uploadService.upload("a.png", broken));

        assertEquals("读取上传文件失败", thrown.getMessage(), "对外的提示文案不能变");
        assertSame(rootCause, thrown.getCause(), "必须保留根因，否则日志里查不到真正原因");
    }

    private static InputStream stream(byte[] content) {
        return new ByteArrayInputStream(content);
    }

    /** 内存存储实现：只记录调用，验证「策略通过后确实委派给存储」 */
    private static final class RecordingStorage implements FileStorage {

        private int calls;
        private Long ownerId;
        private String filename;
        private int size;

        @Override
        public String store(Long ownerId, String originalFilename, byte[] content) {
            this.calls++;
            this.ownerId = ownerId;
            this.filename = originalFilename;
            this.size = content.length;
            return "https://example.test/" + originalFilename;
        }
    }
}
