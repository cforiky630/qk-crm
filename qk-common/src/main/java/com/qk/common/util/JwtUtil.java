package com.qk.common.util;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateField;
import cn.hutool.core.convert.Convert;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.jwt.JWTPayload;
import cn.hutool.jwt.JWTValidator;
import com.qk.common.properties.JwtProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 令牌工具类
 * <p>
 * 基于 Hutool 的 JWT 实现（hutool-all 已在父 POM 中引入），无需额外引依赖。
 */
@Slf4j
@Component
public class JwtUtil {

    private final JwtProperties jwtProperties;

    public JwtUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    /**
     * 生成 JWT 令牌
     *
     * @param claims 自定义载荷，例如 id、username、name
     * @return 令牌字符串
     */
    public String generateToken(Map<String, Object> claims) {
        Map<String, Object> payload = new HashMap<>(claims);
        DateTime now = DateTime.now();
        payload.put(JWTPayload.ISSUED_AT, now);
        payload.put(JWTPayload.EXPIRES_AT, now.offsetNew(DateField.HOUR, jwtProperties.getTtlHours()));
        return JWTUtil.createToken(payload, key());
    }

    /**
     * 校验令牌的签名与有效期
     *
     * @param token 令牌
     * @return 合法返回 true
     */
    public boolean verify(String token) {
        try {
            // 1. 校验签名，防止令牌被篡改
            if (!JWT.of(token).setKey(key()).verify()) {
                return false;
            }
            // 2. 校验有效期（exp / nbf）
            JWTValidator.of(token).validateDate(new Date());
            return true;
        } catch (Exception e) {
            // 令牌格式错误、签名不匹配、已过期等一律视为校验失败
            log.debug("JWT 校验失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从令牌中取出用户ID
     *
     * @param token 令牌
     * @return 用户ID，取不到时返回 null
     */
    public Long getUserId(String token) {
        return Convert.toLong(JWT.of(token).getPayload("id"), null);
    }

    /**
     * 解析令牌载荷（不校验签名）
     *
     * @param token 令牌
     * @return 载荷
     */
    public JWTPayload parsePayload(String token) {
        return JWT.of(token).getPayload();
    }

    private byte[] key() {
        return jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
    }
}
