package com.qk.common.util;

import com.qk.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

        assertEquals(OssTemplate.buildObjectName(7L, ".png", content),
                OssTemplate.buildObjectName(7L, ".png", content),
                "重复上传同一张图必须落到同一个对象上");
    }

    @Test
    void differentContentOrUserProducesDifferentObjectName() {
        byte[] first = "a".getBytes(StandardCharsets.UTF_8);
        byte[] second = "b".getBytes(StandardCharsets.UTF_8);

        assertNotEquals(OssTemplate.buildObjectName(7L, ".png", first),
                OssTemplate.buildObjectName(7L, ".png", second));
        assertNotEquals(OssTemplate.buildObjectName(7L, ".png", first),
                OssTemplate.buildObjectName(8L, ".png", first), "不同用户之间不共享对象");
        assertTrue(OssTemplate.buildObjectName(null, ".png", first).startsWith("images/anonymous/"));
        assertTrue(OssTemplate.buildObjectName(7L, ".png", first).endsWith(".png"));
    }

    /**
     * 读取上传文件失败时必须保留根因
     * <p>
     * 对外提示不能变（前端只看得到「读取上传文件失败」），但日志里要能看到到底为什么失败，
     * 否则超时、断流、被拒绝这些情况在排查时全都长一个样。
     */
    @Test
    void uploadKeepsRootCauseWhenReadingFails() {
        IOException rootCause = new IOException("simulated read failure");
        InputStream broken = new InputStream() {
            @Override
            public int read() throws IOException {
                throw rootCause;
            }
        };

        // 读取失败发生在触碰 OSS 客户端之前，所以两个依赖传 null 也不会被用到
        OssTemplate template = new OssTemplate(null, null);

        BusinessException thrown = assertThrows(BusinessException.class,
                () -> template.upload(1L, "a.png", broken));

        assertEquals("读取上传文件失败", thrown.getMessage(), "对外的提示文案不能变");
        assertSame(rootCause, thrown.getCause(), "必须保留根因，否则日志里查不到真正原因");
    }
}
