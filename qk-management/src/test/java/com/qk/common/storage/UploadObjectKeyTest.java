package com.qk.common.storage;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 上传对象键的布局规则
 * <p>
 * 这是「按天清理」的实现基础：一天的图片都挂在 {@code images/年/月/日/} 前缀下，
 * 因此清理任务能按前缀精确圈定前一天的图片；同时键里有内容哈希，同一天重复上传是覆盖写。
 * 不需要真实 OSS 即可验证。
 */
class UploadObjectKeyTest {

    private static final byte[] CONTENT = "fake-image-bytes".getBytes(StandardCharsets.UTF_8);
    private static final LocalDate DAY = LocalDate.of(2026, 10, 12);

    @Test
    void keyIsOrganisedByDayAndUploader() {
        String key = UploadObjectKey.of(12L, ".png", CONTENT, DAY);

        assertTrue(key.startsWith("images/2026/10/12/12/"), key);
        assertTrue(key.endsWith(".png"), key);
        assertEquals("images/2026/10/12/", UploadObjectKey.dayPrefix(DAY));
    }

    @Test
    void anonymousUploadUsesAnonymousSegment() {
        assertTrue(UploadObjectKey.of(null, ".png", CONTENT, DAY).startsWith("images/2026/10/12/anonymous/"));
    }

    /** 同一天内：同一用户 + 相同内容 → 同一个对象，重复上传只是覆盖写 */
    @Test
    void sameDaySameUserSameContentProducesSameKey() {
        assertEquals(UploadObjectKey.of(7L, ".png", CONTENT, DAY),
                UploadObjectKey.of(7L, ".png", CONTENT, DAY));
    }

    /**
     * 跨天、跨用户、内容不同都会产生不同的键
     * <p>
     * 跨天重复上传同一张图会生成新对象——旧对象只要没被业务引用，第二天就会被清理任务回收，
     * 因此不会长期堆积；这是「按天分目录」换来的取舍。
     */
    @Test
    void differentDayOrUserOrContentProducesDifferentKey() {
        assertNotEquals(UploadObjectKey.of(7L, ".png", CONTENT, DAY),
                UploadObjectKey.of(7L, ".png", CONTENT, DAY.plusDays(1)), "跨天上传同一张图是两个对象");
        assertNotEquals(UploadObjectKey.of(7L, ".png", CONTENT, DAY),
                UploadObjectKey.of(8L, ".png", CONTENT, DAY), "不同用户之间不共享对象");
        assertNotEquals(UploadObjectKey.of(7L, ".png", CONTENT, DAY),
                UploadObjectKey.of(7L, ".png", "other".getBytes(StandardCharsets.UTF_8), DAY));
    }

    /** 清理任务要能从键里读回日期；不符合布局的历史键返回空，而不是抛异常 */
    @Test
    void dayIsReadableFromKey() {
        assertEquals("2026/10/12",
                UploadObjectKey.dayOf(UploadObjectKey.of(1L, ".png", CONTENT, DAY)).orElseThrow());
        assertTrue(UploadObjectKey.dayOf("images/12/abc.png").isEmpty(), "旧布局不能被误判成按天");
        assertTrue(UploadObjectKey.dayOf("other/2026/10/12/x.png").isEmpty());
        assertTrue(UploadObjectKey.dayOf(null).isEmpty());
    }

    /** 同一天的上传都落在同一个前缀下，「只处理前一天的」靠的就是它 */
    @Test
    void allUploadsOfADayShareTheDayPrefix() {
        assertTrue(UploadObjectKey.of(1L, ".png", CONTENT, DAY).startsWith(UploadObjectKey.dayPrefix(DAY)));
        assertTrue(UploadObjectKey.of(999L, ".jpg", "x".getBytes(StandardCharsets.UTF_8), DAY)
                .startsWith(UploadObjectKey.dayPrefix(DAY)));
        assertFalse(UploadObjectKey.of(1L, ".png", CONTENT, DAY.plusDays(1))
                .startsWith(UploadObjectKey.dayPrefix(DAY)));
    }
}
