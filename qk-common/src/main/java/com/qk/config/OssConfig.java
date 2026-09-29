package com.qk.config;

import com.aliyun.sdk.service.oss2.OSSClient;
import com.aliyun.sdk.service.oss2.OSSClientBuilder;
import com.aliyun.sdk.service.oss2.credentials.CredentialsProvider;
import com.aliyun.sdk.service.oss2.credentials.StaticCredentialsProvider;
import com.qk.properties.OssProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OssProperties.class)
public class OssConfig {

    @Bean(destroyMethod = "close")
    public OSSClient ossClient(OssProperties properties) {
        // 使用 application.yml 中 qk.oss 配置的 AccessKey 构建客户端
        CredentialsProvider provider = new StaticCredentialsProvider(
                properties.getAccessKeyId(), properties.getAccessKeySecret());

        OSSClientBuilder builder = OSSClient.newBuilder()
                .credentialsProvider(provider)
                .region(properties.getRegion());

        return builder.build();
    }
}
