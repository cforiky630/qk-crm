package com.qk.service.impl;

import com.qk.config.UploadCleanupProperties;
import com.qk.entity.po.UploadFile;
import com.qk.mapper.UploadFileMapper;
import com.qk.mapper.UserMapper;
import com.qk.service.UploadCleanupService;
import com.qk.service.UploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 上传对象兜底回收实现
 * <p>
 * 「能不能删」以引用扫描为准：只有超过宽限期、且当前没有任何业务数据引用该地址的对象才会被回收。
 * 因此取消新增（从未被引用）、换头像（旧地址不再被引用）、删用户（逻辑删除后不再被引用）
 * 三种场景被同一套规则覆盖，不需要为每个入口单独写删除逻辑。
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "qk.upload.cleanup.enabled", havingValue = "true", matchIfMissing = true)
public class UploadCleanupServiceImpl implements UploadCleanupService {

    private final UploadFileMapper uploadFileMapper;
    private final UserMapper userMapper;
    private final UploadService uploadService;
    private final UploadCleanupProperties properties;

    public UploadCleanupServiceImpl(UploadFileMapper uploadFileMapper, UserMapper userMapper,
                                    UploadService uploadService, UploadCleanupProperties properties) {
        this.uploadFileMapper = uploadFileMapper;
        this.userMapper = userMapper;
        this.uploadService = uploadService;
        this.properties = properties;
    }

    /**
     * 定时入口
     * <p>
     * 单独一个方法而不是直接标注在 {@code cleanupOrphans} 上：避免接口方法 + 注解在代理场景下
     * 被漏扫，也方便测试直接调用业务方法而不经过调度。
     */
    @Scheduled(cron = "${qk.upload.cleanup.cron:0 0 3 * * ?}")
    public void scheduledCleanup() {
        try {
            cleanupOrphans();
        } catch (Exception e) {
            // 兜底任务自己不能把调度线程搞挂，否则会永久停止回收
            log.error("上传对象回收任务执行失败", e);
        }
    }

    @Override
    public int cleanupOrphans() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(properties.getRetentionHours());
        List<UploadFile> candidates = uploadFileMapper.listRecyclable(cutoff, properties.getBatchSize());
        if (candidates.isEmpty()) {
            return 0;
        }
        // 引用集合只查一次，避免对每个候选都扫一遍用户表
        Set<String> referenced = new HashSet<>(userMapper.listImageUrls());

        int recycled = 0;
        int skipped = 0;
        for (UploadFile candidate : candidates) {
            if (referenced.contains(candidate.getUrl())) {
                skipped++;
                continue;
            }
            if (uploadService.recycleIfUnreferenced(candidate.getUrl())) {
                recycled++;
            }
        }
        log.info("上传对象回收完成：候选 {} 个，仍被引用 {} 个，实际回收 {} 个",
                candidates.size(), skipped, recycled);
        return recycled;
    }
}
