package com.qk.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.qk.common.exception.BusinessException;
import com.qk.common.storage.FileStorage;
import com.qk.common.storage.StoredObject;
import com.qk.common.util.UserHolder;
import com.qk.entity.enums.UploadStatus;
import com.qk.entity.po.UploadFile;
import com.qk.mapper.UploadFileMapper;
import com.qk.mapper.UserMapper;
import com.qk.service.UploadService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 上传策略与上传台账单测
 * <p>
 * 不连数据库、不连 OSS：{@link FileStorage} 是端口，测试用内存实现替掉真实对象存储，
 * 台账 Mapper 用 Mockito 替掉。既验证校验规则（白名单、文件头、空内容），
 * 也验证本次新增的「登记台账 / 绑定 / 回收」三条链路。
 */
class UploadServiceImplTest {

    /** PNG 文件头，够魔术字节校验用 */
    private static final byte[] PNG_HEAD = {
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D};

    private final RecordingStorage storage = new RecordingStorage();
    private final UploadFileMapper uploadFileMapper = mock(UploadFileMapper.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final UploadServiceImpl uploadService =
            new UploadServiceImpl(storage, uploadFileMapper, userMapper);

    @AfterEach
    void clearUserContext() {
        UserHolder.removeCurrentUser();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void storesValidImageAndReturnsUrl() {
        UserHolder.setCurrentUser(42L);

        String url = uploadService.upload("photo.png", stream(PNG_HEAD));

        assertEquals("https://example.test/photo.png", url);
        assertEquals(42L, storage.ownerId, "上传人必须透传给存储实现");
        assertEquals("photo.png", storage.filename);
        assertEquals(PNG_HEAD.length, storage.size);
    }

    /**
     * 上传成功必须登记台账，且初始状态是「临时」：这是「取消新增后能回收」的前提
     */
    @Test
    void registersTemporaryLedgerRowOnUpload() {
        UserHolder.setCurrentUser(42L);
        when(uploadFileMapper.findByObjectKey(anyString())).thenReturn(null);

        uploadService.upload("photo.png", stream(PNG_HEAD));

        ArgumentCaptor<UploadFile> captor = ArgumentCaptor.forClass(UploadFile.class);
        verify(uploadFileMapper).insert(captor.capture());
        UploadFile recorded = captor.getValue();
        assertEquals(UploadStatus.TEMP.getCode(), recorded.getStatus());
        assertEquals(42L, recorded.getUploaderId());
        assertEquals("https://example.test/photo.png", recorded.getUrl());
        assertEquals(DigestUtil.md5Hex(PNG_HEAD), recorded.getContentMd5());
        assertEquals(PNG_HEAD.length, recorded.getSize());
    }

    /**
     * 内容寻址：重复上传同一对象只刷新台账（重置宽限期），不再新增一条
     */
    @Test
    void reuploadRefreshesExistingLedgerRowInsteadOfInserting() {
        UploadFile existing = new UploadFile();
        existing.setId(9L);
        when(uploadFileMapper.findByObjectKey(anyString())).thenReturn(existing);

        uploadService.upload("photo.png", stream(PNG_HEAD));

        verify(uploadFileMapper).refreshOnReupload(eq(9L), any());
        verify(uploadFileMapper, never()).insert(any(UploadFile.class));
    }

    /**
     * 台账写不进去时必须把刚存进去的对象删掉：否则对象就成了永远没人认领的孤儿
     */
    @Test
    void compensatesWhenLedgerInsertFails() {
        when(uploadFileMapper.findByObjectKey(anyString())).thenReturn(null);
        when(uploadFileMapper.insert(any(UploadFile.class))).thenThrow(new RuntimeException("db down"));

        BusinessException e = assertThrows(BusinessException.class,
                () -> uploadService.upload("photo.png", stream(PNG_HEAD)));

        assertEquals("图片上传失败，请稍后重试", e.getMessage());
        assertTrue(storage.deleted.contains(storage.lastObjectKey), "台账没记上，对象必须补偿删除");
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

    /** 绑定：把台账记到业务数据名下 */
    @Test
    void bindMarksLedgerRowAsBound() {
        when(uploadFileMapper.findByUrl("https://example.test/a.png"))
                .thenReturn(ledger(3L, UploadStatus.TEMP, "https://example.test/a.png"));

        uploadService.bindImage("https://example.test/a.png", UploadService.REF_TYPE_USER, 7L);

        verify(uploadFileMapper).markBound(eq(3L), eq(UploadService.REF_TYPE_USER), eq(7L), any());
    }

    /** 外链 / 历史数据不在台账里，绑定要静默跳过，不能因为找不到就报错 */
    @Test
    void bindIgnoresUrlNotInLedger() {
        when(uploadFileMapper.findByUrl(anyString())).thenReturn(null);

        uploadService.bindImage("https://cdn.example.com/legacy.png", UploadService.REF_TYPE_USER, 7L);

        verify(uploadFileMapper, never()).markBound(anyLong(), anyString(), anyLong(), any());
    }

    /** 仍被引用：不删，并把状态修正回「已绑定」 */
    @Test
    void recycleKeepsObjectThatIsStillReferenced() {
        when(uploadFileMapper.findByUrl(anyString()))
                .thenReturn(ledger(3L, UploadStatus.TEMP, "https://example.test/a.png"));
        when(userMapper.listImageUrls()).thenReturn(List.of("https://example.test/a.png"));

        assertFalse(uploadService.recycleIfUnreferenced("https://example.test/a.png"));
        assertTrue(storage.deleted.isEmpty(), "仍被引用的对象不能删除");
        verify(uploadFileMapper).markBound(eq(3L), any(), any(), any());
    }

    /** 无人引用：抢占后删除对象 */
    @Test
    void recycleDeletesObjectThatIsNoLongerReferenced() {
        when(uploadFileMapper.findByUrl(anyString()))
                .thenReturn(ledger(3L, UploadStatus.BOUND, "https://example.test/a.png"));
        when(userMapper.listImageUrls()).thenReturn(List.of());
        when(uploadFileMapper.claimForRecycle(3L)).thenReturn(1);

        assertTrue(uploadService.recycleIfUnreferenced("https://example.test/a.png"));
        assertTrue(storage.deleted.contains("images/42/a.png"));
    }

    /** 删除对象失败：状态退回、返回 false，留待下一轮重试，绝不能当成已回收 */
    @Test
    void recycleRevertsWhenStorageDeleteFails() {
        when(uploadFileMapper.findByUrl(anyString()))
                .thenReturn(ledger(3L, UploadStatus.BOUND, "https://example.test/a.png"));
        when(userMapper.listImageUrls()).thenReturn(List.of());
        when(uploadFileMapper.claimForRecycle(3L)).thenReturn(1);
        storage.failDelete = true;

        assertFalse(uploadService.recycleIfUnreferenced("https://example.test/a.png"));
        verify(uploadFileMapper).revertRecycle(3L);
    }

    /** 已被其它实例回收：不再重复删 */
    @Test
    void recycleSkipsAlreadyRecycledRow() {
        when(uploadFileMapper.findByUrl(anyString()))
                .thenReturn(ledger(3L, UploadStatus.RECYCLED, "https://example.test/a.png"));

        assertFalse(uploadService.recycleIfUnreferenced("https://example.test/a.png"));
        assertTrue(storage.deleted.isEmpty(), "已回收的对象不应重复删除");
    }

    /**
     * 事务内释放：必须推迟到提交后执行
     * <p>
     * 否则「业务事务回滚」会变成「用户没删掉、头像却已经没了」。
     */
    @Test
    void releaseInsideTransactionDefersUntilCommit() {
        when(uploadFileMapper.findByUrl(anyString()))
                .thenReturn(ledger(3L, UploadStatus.BOUND, "https://example.test/a.png"));
        when(userMapper.listImageUrls()).thenReturn(List.of());
        when(uploadFileMapper.claimForRecycle(3L)).thenReturn(1);

        TransactionSynchronizationManager.initSynchronization();
        uploadService.releaseImage("https://example.test/a.png");

        assertTrue(storage.deleted.isEmpty(), "提交之前不能删对象");

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);

        assertTrue(storage.deleted.contains("images/42/a.png"), "提交之后才删");
    }

    /** 事务外的释放立即执行（定时任务路径也走这一个入口） */
    @Test
    void releaseOutsideTransactionDeletesImmediately() {
        when(uploadFileMapper.findByUrl(anyString()))
                .thenReturn(ledger(3L, UploadStatus.TEMP, "https://example.test/a.png"));
        when(userMapper.listImageUrls()).thenReturn(List.of());
        when(uploadFileMapper.claimForRecycle(3L)).thenReturn(1);

        uploadService.releaseImage("https://example.test/a.png");

        assertTrue(storage.deleted.contains("images/42/a.png"));
    }

    /** 释放失败不能影响业务：对象存储抛异常时也不能把异常抛给调用方 */
    @Test
    void releaseNeverPropagatesStorageFailure() {
        when(uploadFileMapper.findByUrl(anyString()))
                .thenReturn(ledger(3L, UploadStatus.BOUND, "https://example.test/a.png"));
        when(userMapper.listImageUrls()).thenReturn(List.of());
        when(uploadFileMapper.claimForRecycle(3L)).thenReturn(1);
        storage.failDelete = true;

        uploadService.releaseImage("https://example.test/a.png");

        verify(uploadFileMapper).revertRecycle(3L);
    }

    private static UploadFile ledger(Long id, UploadStatus status, String url) {
        UploadFile file = new UploadFile();
        file.setId(id);
        file.setStatus(status.getCode());
        file.setUrl(url);
        file.setObjectKey("images/42/a.png");
        return file;
    }

    private static InputStream stream(byte[] content) {
        return new ByteArrayInputStream(content);
    }

    /** 内存存储实现：记录调用，并允许模拟删除失败 */
    private static final class RecordingStorage implements FileStorage {

        private int calls;
        private Long ownerId;
        private String filename;
        private int size;
        private String lastObjectKey;
        private final List<String> deleted = new ArrayList<>();
        private boolean failDelete;

        @Override
        public StoredObject store(Long ownerId, String originalFilename, byte[] content) {
            this.calls++;
            this.ownerId = ownerId;
            this.filename = originalFilename;
            this.size = content.length;
            this.lastObjectKey = "images/" + ownerId + "/a.png";
            return new StoredObject(
                    "https://example.test/" + originalFilename,
                    lastObjectKey,
                    DigestUtil.md5Hex(content),
                    content.length,
                    "image/png");
        }

        @Override
        public void delete(String objectKey) {
            if (failDelete) {
                throw new IllegalStateException("simulated oss failure");
            }
            deleted.add(objectKey);
        }
    }
}
