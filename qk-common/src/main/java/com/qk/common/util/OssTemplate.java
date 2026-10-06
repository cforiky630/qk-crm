package com.qk.common.util;

import com.aliyun.sdk.service.oss2.OSSClient;
import com.aliyun.sdk.service.oss2.models.PutObjectRequest;
import com.aliyun.sdk.service.oss2.transport.BinaryData;
import com.qk.common.exception.BusinessException;
import com.qk.common.exception.ErrorCode;
import com.qk.common.properties.OssProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cn.hutool.crypto.digest.DigestUtil;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Component
public class OssTemplate {

    /** 扩展名 → Content-Type，未登记的扩展名统一按二进制流 */
    private static final Map<String, String> CONTENT_TYPES = Map.of(
            ".bmp", "image/bmp",
            ".gif", "image/gif",
            ".jpeg", "image/jpeg",
            ".jpg", "image/jpeg",
            ".png", "image/png",
            ".webp", "image/webp");

    private final OSSClient ossClient;
    private final OssProperties ossProperties;

    @Autowired
    public OssTemplate(OSSClient ossClient, OssProperties ossProperties) {
        this.ossClient = ossClient;
        this.ossProperties = ossProperties;
    }

    /**
     * 上传图片到 OSS
     *
     * @param userId      上传人ID，用于按用户隔离对象路径，便于后续追溯与清理
     * @param fileName    原始文件名，仅用于取扩展名
     * @param inputStream 文件流
     * @return 可直接访问的 https 地址
     */
    public String upload(Long userId, String fileName, InputStream inputStream) {
        // 与控制器共用同一套取法：没有扩展名时返回空串，这里再转成业务提示
        String suffix = FileNameUtil.extensionOf(fileName);
        if (suffix.isEmpty()) {
            throw new BusinessException(ErrorCode.OSS_FILENAME_NO_EXTENSION);
        }
        byte[] content;
        try {
            content = inputStream.readAllBytes();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.OSS_READ_FAILED, e);
        }

        // 对象名按「内容哈希」生成，而不是随机 UUID：
        // 同一用户重复上传同一张图会落到同一个对象上（覆盖写），
        // 因此双击提交或网络重试都不会在 OSS 里堆积孤儿文件，上传天然幂等。
        String objectName = buildObjectName(userId, suffix, content);

        PutObjectRequest request = PutObjectRequest.newBuilder().bucket(ossProperties.getBucketName()).key(objectName).body(BinaryData.fromStream(new ByteArrayInputStream(content))).contentType(getContentType(suffix)).build();

        ossClient.putObject(request);

        return "https://" + ossProperties.getBucketName() + ".oss-" + ossProperties.getRegion() + ".aliyuncs.com/" + objectName;
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

    private String getContentType(String suffix) {
        return CONTENT_TYPES.getOrDefault(suffix.toLowerCase(), "application/octet-stream");
    }
}
