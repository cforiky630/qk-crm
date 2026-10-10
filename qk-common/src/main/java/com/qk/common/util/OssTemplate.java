package com.qk.common.util;

import com.aliyun.sdk.service.oss2.OSSClient;
import com.aliyun.sdk.service.oss2.models.DeleteObjectRequest;
import com.aliyun.sdk.service.oss2.models.PutObjectRequest;
import com.aliyun.sdk.service.oss2.transport.BinaryData;
import com.qk.common.exception.BusinessException;
import com.qk.common.exception.ErrorCode;
import com.qk.common.properties.OssProperties;
import com.qk.common.storage.FileStorage;
import com.qk.common.storage.ImageFormat;
import com.qk.common.storage.StoredObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cn.hutool.crypto.digest.DigestUtil;
import java.io.ByteArrayInputStream;

/**
 * 文件存储出口的阿里云 OSS 实现
 * <p>
 * 只负责「对象名怎么拼、Content-Type 填什么、怎么调 SDK」，
 * 上传策略（白名单、文件头校验、大小限制）在服务层的 {@code UploadService} 里，
 * 因此换存储或改策略互不影响。
 */
@Component
public class OssTemplate implements FileStorage {

    private final OSSClient ossClient;
    private final OssProperties ossProperties;

    @Autowired
    public OssTemplate(OSSClient ossClient, OssProperties ossProperties) {
        this.ossClient = ossClient;
        this.ossProperties = ossProperties;
    }

    @Override
    public StoredObject store(Long ownerId, String originalFilename, byte[] content) {
        // 扩展名由调用方（UploadService）先校验过；这里再判一次是为了让适配器自身也站得住，
        // 不依赖"调用方一定校验过"这个前提
        String suffix = FileNameUtil.extensionOf(originalFilename);
        if (suffix.isEmpty()) {
            throw new BusinessException(ErrorCode.OSS_FILENAME_NO_EXTENSION);
        }

        // 对象名按「内容哈希」生成，而不是随机 UUID：
        // 同一用户重复上传同一张图会落到同一个对象上（覆盖写），
        // 因此双击提交或网络重试都不会在 OSS 里堆积孤儿文件，上传天然幂等。
        String objectName = buildObjectName(ownerId, suffix, content);
        String contentType = contentTypeOf(suffix);

        PutObjectRequest request = PutObjectRequest.newBuilder()
                .bucket(ossProperties.getBucketName())
                .key(objectName)
                .body(BinaryData.fromStream(new ByteArrayInputStream(content)))
                .contentType(contentType)
                .build();

        ossClient.putObject(request);

        String url = "https://" + ossProperties.getBucketName() + ".oss-" + ossProperties.getRegion() + ".aliyuncs.com/" + objectName;
        return new StoredObject(url, objectName, DigestUtil.md5Hex(content), content.length, contentType);
    }

    /**
     * 删除对象
     * <p>
     * OSS 的 DeleteObject 本身就是幂等的：对象不存在时同样返回成功，因此重复调用安全。
     */
    @Override
    public void delete(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }
        DeleteObjectRequest request = DeleteObjectRequest.newBuilder()
                .bucket(ossProperties.getBucketName())
                .key(objectKey)
                .build();
        ossClient.deleteObject(request);
    }

    /**
     * 内容寻址的对象名：{@code images/{userId}/{内容MD5}{扩展名}}
     * <p>
     * 相同的上传人 + 相同的图片内容 → 同一个对象名，这正是上传幂等的实现方式。
     */
    static String buildObjectName(Long userId, String suffix, byte[] content) {
        String userPath = userId == null ? "anonymous" : String.valueOf(userId);
        return "images/" + userPath + "/" + DigestUtil.md5Hex(content) + suffix;
    }

    /** Content-Type 与白名单同源（{@link ImageFormat}）；未登记的扩展名按二进制流处理 */
    private String contentTypeOf(String suffix) {
        return ImageFormat.ofExtension(suffix)
                .map(ImageFormat::contentType)
                .orElse("application/octet-stream");
    }
}
