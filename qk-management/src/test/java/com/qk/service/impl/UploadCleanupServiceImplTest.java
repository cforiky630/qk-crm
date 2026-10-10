package com.qk.service.impl;

import com.qk.config.UploadCleanupProperties;
import com.qk.entity.enums.UploadStatus;
import com.qk.entity.po.UploadFile;
import com.qk.mapper.UploadFileMapper;
import com.qk.mapper.UserMapper;
import com.qk.service.UploadService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 上传对象兜底回收单测
 * <p>
 * 不连数据库、不连 OSS：台账与引用查询用 Mockito 替掉，只验证「候选筛选 + 跳过仍被引用 + 委派回收」。
 */
class UploadCleanupServiceImplTest {

    private final UploadFileMapper uploadFileMapper = mock(UploadFileMapper.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final UploadService uploadService = mock(UploadService.class);
    private final UploadCleanupProperties properties = new UploadCleanupProperties();
    private final UploadCleanupServiceImpl cleanupService =
            new UploadCleanupServiceImpl(uploadFileMapper, userMapper, uploadService, properties);

    /** 仍被业务数据引用的候选必须跳过，只回收无人引用的那个 */
    @Test
    void skipsReferencedCandidateAndRecyclesTheRest() {
        UploadFile referenced = candidate("https://example.test/used.png");
        UploadFile orphan = candidate("https://example.test/orphan.png");
        when(uploadFileMapper.listRecyclable(any(), anyInt())).thenReturn(List.of(referenced, orphan));
        when(userMapper.listImageUrls()).thenReturn(List.of("https://example.test/used.png"));
        when(uploadService.recycleIfUnreferenced("https://example.test/orphan.png")).thenReturn(true);

        assertEquals(1, cleanupService.cleanupOrphans());
        verify(uploadService, never()).recycleIfUnreferenced("https://example.test/used.png");
    }

    /** 没有候选时不做任何多余查询 */
    @Test
    void returnsZeroWhenThereIsNoCandidate() {
        when(uploadFileMapper.listRecyclable(any(), anyInt())).thenReturn(List.of());

        assertEquals(0, cleanupService.cleanupOrphans());
        verify(userMapper, never()).listImageUrls();
    }

    /** 调度入口自己不能把调度线程搞挂，否则回收会永久停止 */
    @Test
    void scheduledCleanupSwallowsFailure() {
        when(uploadFileMapper.listRecyclable(any(), anyInt())).thenThrow(new RuntimeException("db down"));

        cleanupService.scheduledCleanup();

        verify(uploadService, never()).recycleIfUnreferenced(anyString());
    }

    private static UploadFile candidate(String url) {
        UploadFile file = new UploadFile();
        file.setStatus(UploadStatus.TEMP.getCode());
        file.setUrl(url);
        file.setObjectKey("images/1/x.png");
        file.setCreateTime(LocalDateTime.now().minusDays(2));
        return file;
    }
}
