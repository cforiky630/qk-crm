package com.qk.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("qk.oss")
@Data
public class OssProperties {
    private String region;          // 替代 V1 的 endpoint
    private String bucketName;
    private String accessKeyId;
    private String accessKeySecret;
}