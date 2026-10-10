package com.qk;

import com.qk.common.properties.OssProperties;
import com.qk.common.util.OssTemplate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Base64;

/**
 * 真实连接阿里云 OSS 的联调测试
 * <p>
 * 会真实上传文件到 OSS 并写入一条对象记录，依赖外网与真实密钥，因此默认跳过。
 * 需要验证上传功能时，去掉 {@code @Disabled} 后单独执行：
 * <pre>mvn -Dtest=OssUploadManualTest test</pre>
 * 上传成功后可用浏览器或 curl 直接访问控制台打印的 UPLOADED_URL 确认图片可见。
 */
@Disabled("依赖外网与真实 OSS 密钥，默认不纳入自动化测试；需要时手动执行")
@SpringBootTest
class OssUploadManualTest {

    /** 1x1 透明 PNG，用于验证上传链路与 contentType 设置 */
    private static final String PNG_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==";

    @Autowired
    private OssProperties ossProperties;

    @Autowired
    private OssTemplate ossTemplate;

    @Test
    void uploadPngToOss() {
        System.out.println("CONFIG_REGION=" + ossProperties.getRegion()
                + " CONFIG_BUCKET=" + ossProperties.getBucketName());

        byte[] bytes = Base64.getDecoder().decode(PNG_BASE64);
        String url = ossTemplate.store(1L, "codex-upload-test.png", bytes).url();
        System.out.println("UPLOADED_URL=" + url);
        System.out.println("UPLOAD_BYTES=" + bytes.length);

        Assertions.assertTrue(url.startsWith("https://" + ossProperties.getBucketName() + ".oss-"),
                "返回的访问路径应以 bucket 的域名开头");
    }

    @Test
    void credentialsAreConfigured() {
        // 配置检查：不需要外网，避免密钥被悄悄改错
        Assertions.assertNotNull(ossProperties.getRegion());
        Assertions.assertNotNull(ossProperties.getBucketName());
        if (ossProperties.getAccessKeyId() != null && !ossProperties.getAccessKeyId().isBlank()) {
            System.out.println("AK_HEAD=" + head(ossProperties.getAccessKeyId()));
        }
    }

    private String head(String value) {
        return value == null || value.length() < 8 ? "null" : value.substring(0, 8) + "****";
    }
}
