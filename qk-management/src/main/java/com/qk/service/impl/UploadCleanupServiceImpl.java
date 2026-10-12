package com.qk.service.impl;

import com.qk.config.UploadCleanupProperties;
import com.qk.common.storage.UploadObjectKey;
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
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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
        // 正常情况（每天跑一次、宽限期 24 小时）这里命中的就是「前一天上传的」那批；
        // 取「所有已到期的」而不是死盯前一天，是为了某天漏跑时下一次能自动补上。
        return recycleAll(candidates, "已到期未回收");
    }

    @Override
    public int cleanupDay(LocalDate day) {
        String prefix = UploadObjectKey.dayPrefix(day);
        List<UploadFile> candidates = uploadFileMapper.listByObjectKeyPrefix(prefix, properties.getBatchSize());
        return recycleAll(candidates, prefix);
    }

    /**
     * 统一处理一批候选：仍被业务数据引用的留着（顺手修正状态），无人引用的回收
     * <p>
     * 引用集合整批只查一次；日志按对象所属日期汇总，让「按天清理」这件事在日志里看得见。
     */
    private int recycleAll(List<UploadFile> candidates, String scope) {
        if (candidates.isEmpty()) {
            log.info("上传对象回收（{}）：没有需要处理的候选", scope);
            return 0;
        }
        // 引用集合只查一次，避免对每个候选都扫一遍用户表
        Set<String> referenced = new HashSet<>(userMapper.listImageUrls());

        int recycled = 0;
        Map<String, Integer> candidatesByDay = new TreeMap<>();
        for (UploadFile candidate : candidates) {
            candidatesByDay.merge(UploadObjectKey.dayOf(candidate.getObjectKey()).orElse("非按天布局"), 1, Integer::sum);
            if (referenced.contains(candidate.getUrl())) {
                continue;
            }
            if (uploadService.recycleIfUnreferenced(candidate.getUrl())) {
                recycled++;
            }
        }
        log.info("上传对象回收（{}）：候选 {} 个（按日期 {}），实际回收 {} 个",
                scope, candidates.size(), candidatesByDay, recycled);
        return recycled;
    }
}
