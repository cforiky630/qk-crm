package com.qk.common.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 上传对象名的内容寻址规则
 * <p>
 * 这是「重复上传不产生孤儿文件」的实现基础：同一用户 + 相同内容 → 同一对象名，
 * 因此重复上传只是覆盖写。不需要真实 OSS 即可验证。
 */
class OssTemplateTest {

    @Test
    void sameUserWithSameContentProducesSameObjectName() {
        byte[] content = "fake-image-bytes".getBytes(StandardCharsets.UTF_8);

        assertEquals(OssTemplate.buildObjectName(7, ".png", content),
                OssTemplate.buildObjectName(7, ".png", content),
                "重复上传同一张图必须落到同一个对象上");
    }

    @Test
    void differentContentOrUserProducesDifferentObjectName() {
        byte[] first = "a".getBytes(StandardCharsets.UTF_8);
        byte[] second = "b".getBytes(StandardCharsets.UTF_8);

        assertNotEquals(OssTemplate.buildObjectName(7, ".png", first),
                OssTemplate.buildObjectName(7, ".png", second));
        assertNotEquals(OssTemplate.buildObjectName(7, ".png", first),
                OssTemplate.buildObjectName(8, ".png", first), "不同用户之间不共享对象");
        assertTrue(OssTemplate.buildObjectName(null, ".png", first).startsWith("images/anonymous/"));
        assertTrue(OssTemplate.buildObjectName(7, ".png", first).endsWith(".png"));
    }
}
