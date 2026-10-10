package com.qk.service.impl;

import cn.hutool.core.util.StrUtil;
import com.qk.common.exception.BusinessException;
import com.qk.common.exception.ErrorCode;
import com.qk.common.storage.FileStorage;
import com.qk.common.storage.ImageFormat;
import com.qk.common.storage.StoredObject;
import com.qk.common.util.FileNameUtil;
import com.qk.common.util.UserHolder;
import com.qk.entity.enums.UploadStatus;
import com.qk.entity.po.UploadFile;
import com.qk.mapper.UploadFileMapper;
import com.qk.mapper.UserMapper;
import com.qk.service.UploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * 图片上传实现
 * <p>
 * 三道校验按「代价从低到高」排序，避免为明显不合法的请求把整个请求体读进内存：
 * 扩展名白名单 → 内容非空 → 文件头（魔术字节）与扩展名一致。
 * 只校验扩展名时，把脚本改名成 .png 就能通过，因此文件头校验是必要的一道。
 * <p>
 * 通过后交给 {@link FileStorage} 端口，具体存哪里（OSS / 本地 / MinIO）由实现决定。
 * <p>
 * 上传成功后登记 {@code upload_file} 台账，是「取消新增不再留脏数据」的基础：
 * 没有台账就无从知道哪些对象是系统传的、更无从判断它有没有被引用。
 */
@Slf4j
@Service
public class UploadServiceImpl implements UploadService {

    private final FileStorage fileStorage;
    private final UploadFileMapper uploadFileMapper;
    private final UserMapper userMapper;

    public UploadServiceImpl(FileStorage fileStorage, UploadFileMapper uploadFileMapper, UserMapper userMapper) {
        this.fileStorage = fileStorage;
        this.uploadFileMapper = uploadFileMapper;
        this.userMapper = userMapper;
    }

    @Override
    public String upload(String originalFilename, InputStream content) {
        String suffix = FileNameUtil.extensionOf(originalFilename).toLowerCase(Locale.ROOT);
        ImageFormat format = ImageFormat.ofExtension(suffix)
                .orElseThrow(() -> new BusinessException(ErrorCode.UPLOAD_IMAGE_TYPE_UNSUPPORTED));

        byte[] bytes = readAllBytes(content);
        if (bytes.length == 0) {
            throw new BusinessException(ErrorCode.UPLOAD_IMAGE_REQUIRED);
        }
        if (!format.matches(bytes)) {
            throw new BusinessException(ErrorCode.UPLOAD_IMAGE_CONTENT_INVALID);
        }

        Long uploaderId = UserHolder.getCurrentUser();
        log.info("文件上传开始：{}，上传人：{}，大小：{} 字节", originalFilename, uploaderId, bytes.length);
        StoredObject stored = fileStorage.store(uploaderId, originalFilename, bytes);
        recordUpload(stored, uploaderId);
        log.info("文件上传完成：{}", stored.url());
        return stored.url();
    }

    @Override
    public void bindImage(String url, String refType, Long refId) {
        if (StrUtil.isBlank(url)) {
            return;
        }
        try {
            UploadFile file = uploadFileMapper.findByUrl(url);
            if (file == null) {
                // 外链或历史数据不归台账管，静默跳过即可
                log.debug("图片不在上传台账中，跳过绑定：{}", url);
                return;
            }
            if (UploadStatus.RECYCLED.getCode().equals(file.getStatus())) {
                // 对象已被回收却又被提交上来：图片实际已不可访问，记一条日志便于排查
                log.warn("图片已被回收，无法绑定：url={}", url);
                return;
            }
            uploadFileMapper.markBound(file.getId(), refType, refId, LocalDateTime.now());
        } catch (Exception e) {
            // 绑定只是台账记账，失败不能影响业务写入；漏记的绑定会由回收任务按引用重新修正
            log.warn("上传台账绑定失败（不影响业务）：url={}", url, e);
        }
    }

    @Override
    public void releaseImage(String url) {
        if (StrUtil.isBlank(url)) {
            return;
        }
        // 事务内调用：推迟到提交后执行，保证「业务真的删成了」才删对象
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    recycleQuietly(url);
                }
            });
            return;
        }
        recycleQuietly(url);
    }

    @Override
    public boolean recycleIfUnreferenced(String url) {
        if (StrUtil.isBlank(url)) {
            return false;
        }
        UploadFile file = uploadFileMapper.findByUrl(url);
        if (file == null) {
            log.debug("对象不在上传台账中，跳过回收：{}", url);
            return false;
        }
        if (UploadStatus.RECYCLED.getCode().equals(file.getStatus())) {
            return false;
        }
        // 仍被引用：顺手把状态修正回「已绑定」，避免每轮都重复扫描这条
        if (isReferenced(url)) {
            uploadFileMapper.markBound(file.getId(), file.getRefType(), file.getRefId(), LocalDateTime.now());
            return false;
        }
        // 原子抢占，多实例同时回收时只有一个能开始删
        if (uploadFileMapper.claimForRecycle(file.getId()) == 0) {
            return false;
        }
        // 抢占期间可能刚被重新绑定，以「仍被引用」为准，宁可留到下一轮也不误删
        if (isReferenced(url)) {
            uploadFileMapper.revertRecycle(file.getId());
            return false;
        }
        try {
            fileStorage.delete(file.getObjectKey());
            log.info("回收上传对象：{}", file.getObjectKey());
            return true;
        } catch (Exception e) {
            // 删除失败把状态退回去，下一轮重试；绝不能因为删不掉就把它当成已回收
            uploadFileMapper.revertRecycle(file.getId());
            log.warn("删除对象存储文件失败，留待下次回收：objectKey={}", file.getObjectKey(), e);
            return false;
        }
    }

    /**
     * 读取上传流
     * <p>
     * 读取失败要保留根因（超时、断流、被拒绝在日志里必须能区分），
     * 对外的提示仍是固定的「读取上传文件失败」。
     */
    private byte[] readAllBytes(InputStream content) {
        if (content == null) {
            throw new BusinessException(ErrorCode.UPLOAD_IMAGE_REQUIRED);
        }
        try {
            return content.readAllBytes();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.OSS_READ_FAILED, e);
        }
    }

    /**
     * 登记上传台账
     * <p>
     * 内容寻址下同一对象只登记一条：已存在则刷新为「临时」并重算宽限期。
     * 台账写不进去时把刚存进去的对象删掉再抛错 —— 否则对象就成了永远没人认领的孤儿，
     * 正好回到这次要修的问题。
     */
    private void recordUpload(StoredObject stored, Long uploaderId) {
        UploadFile existing = uploadFileMapper.findByObjectKey(stored.objectKey());
        try {
            if (existing == null) {
                UploadFile file = new UploadFile();
                file.setObjectKey(stored.objectKey());
                file.setUrl(stored.url());
                file.setUploaderId(uploaderId);
                file.setContentMd5(stored.contentMd5());
                file.setSize(stored.size());
                file.setContentType(stored.contentType());
                file.setStatus(UploadStatus.TEMP.getCode());
                file.setRetryCount(0);
                uploadFileMapper.insert(file);
            } else {
                uploadFileMapper.refreshOnReupload(existing.getId(), LocalDateTime.now());
            }
        } catch (Exception e) {
            if (existing == null) {
                deleteQuietly(stored.objectKey());
            }
            throw new BusinessException(ErrorCode.UPLOAD_RECORD_FAILED, e);
        }
    }

    /** 该地址是否仍被任何业务数据引用（当前只有用户头像；新增图片来源列时同步补这里） */
    private boolean isReferenced(String url) {
        return userMapper.listImageUrls().contains(url);
    }

    /** 尽力而为的回收：任何异常都只记日志，不向调用方传播 */
    private void recycleQuietly(String url) {
        try {
            recycleIfUnreferenced(url);
        } catch (Exception e) {
            log.warn("同步回收图片失败，留待定时任务兜底：url={}", url, e);
        }
    }

    /** 尽力而为的删除，用于「台账没记上，把对象也一并清掉」的补偿 */
    private void deleteQuietly(String objectKey) {
        try {
            fileStorage.delete(objectKey);
        } catch (Exception e) {
            log.warn("补偿删除对象失败：objectKey={}", objectKey, e);
        }
    }
}
