package com.qk.service.impl;

import com.qk.common.exception.BusinessException;
import com.qk.common.exception.ErrorCode;
import com.qk.common.storage.FileStorage;
import com.qk.common.storage.ImageFormat;
import com.qk.common.util.FileNameUtil;
import com.qk.common.util.UserHolder;
import com.qk.service.UploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/**
 * 图片上传实现
 * <p>
 * 三道校验按「代价从低到高」排序，避免为明显不合法的请求把整个请求体读进内存：
 * 扩展名白名单 → 内容非空 → 文件头（魔术字节）与扩展名一致。
 * 只校验扩展名时，把脚本改名成 .png 就能通过，因此文件头校验是必要的一道。
 * <p>
 * 通过后交给 {@link FileStorage} 端口，具体存哪里（OSS / 本地 / MinIO）由实现决定。
 */
@Slf4j
@Service
public class UploadServiceImpl implements UploadService {

    private final FileStorage fileStorage;

    public UploadServiceImpl(FileStorage fileStorage) {
        this.fileStorage = fileStorage;
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
        String url = fileStorage.store(uploaderId, originalFilename, bytes);
        log.info("文件上传完成：{}", url);
        return url;
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
}
