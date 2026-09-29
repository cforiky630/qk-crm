package com.qk.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置，对应 application.yml 中的 qk.jwt
 */
@Component
@ConfigurationProperties("qk.jwt")
@Data
public class JwtProperties {

    /** 签名密钥，生产环境务必通过环境变量注入，且长度不少于 32 字节 */
    private String secret;

    /** 令牌有效期，单位：小时 */
    private Integer ttlHours = 24;
}
