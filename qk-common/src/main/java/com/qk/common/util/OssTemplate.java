package com.qk.common.util;

import com.aliyun.sdk.service.oss2.OSSClient;
import com.aliyun.sdk.service.oss2.models.PutObjectRequest;
import com.aliyun.sdk.service.oss2.transport.BinaryData;
import com.qk.common.exception.BusinessException;
import com.qk.common.properties.OssProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@Component
public class OssTemplate {

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
    public String upload(Integer userId, String fileName, InputStream inputStream) {
        int dotIndex = fileName == null ? -1 : fileName.lastIndexOf(".");
        if (dotIndex < 0) {
            throw new BusinessException("文件名缺少扩展名，无法识别图片格式");
        }
        String suffix = fileName.substring(dotIndex);
        String datePath = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
        // 对象名带上上传人：images/{userId}/yyyy/MM/dd/{uuid}.ext
        String userPath = userId == null ? "anonymous" : String.valueOf(userId);
        String objectName = "images/" + userPath + "/" + datePath + "/"
                + UUID.randomUUID().toString().replace("-", "") + suffix;

        PutObjectRequest request = PutObjectRequest.newBuilder().bucket(ossProperties.getBucketName()).key(objectName).body(BinaryData.fromStream(inputStream)).contentType(getContentType(suffix)).build();

        ossClient.putObject(request);

        return "https://" + ossProperties.getBucketName() + ".oss-" + ossProperties.getRegion() + ".aliyuncs.com/" + objectName;
    }

    private String getContentType(String suffix) {
        if (suffix.equalsIgnoreCase(".bmp")) {
            return "image/bmp";
        }
        if (suffix.equalsIgnoreCase(".gif")) {
            return "image/gif";
        }
        if (suffix.equalsIgnoreCase(".jpeg") || suffix.equalsIgnoreCase(".jpg")) {
            return "image/jpeg";
        }
        if (suffix.equalsIgnoreCase(".png")) {
            return "image/png";
        }
        if (suffix.equalsIgnoreCase(".webp")) {
            return "image/webp";
        }
        return "application/octet-stream";
    }
}
