package com.qk.common.storage;

import cn.hutool.crypto.digest.DigestUtil;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * 上传对象在对象存储里的键（路径）布局
 * <p>
 * 形如 {@code images/2026/10/12/12/{内容MD5}{后缀}}：年 / 月 / 日 / 上传人ID / 内容哈希。
 * <ul>
 *   <li><b>按天分目录</b>：一天的图片都在同一个前缀下，清理任务可以按前缀精确圈定「某一天上传的」，
 *       人工排查、按天对账、以及在对象存储侧配置按前缀的生命周期规则都方便；</li>
 *   <li><b>按上传人隔离</b>：同一张图由不同用户上传就是不同对象，不会互相影响；</li>
 *   <li><b>内容寻址</b>：<b>同一天内</b>同一用户重复上传同一张图会落到同一个对象上（覆盖写），
 *       因此双击提交、网络重试都是幂等的。跨天上传同一张图会生成新对象——旧对象只要没被业务引用，
 *       第二天就会被清理任务回收，不会长期堆积。</li>
 * </ul>
 * 布局只有这一处定义：写入（{@link #of}）与清理（{@link #dayPrefix} / {@link #dayOf}）都用它，
 * 避免"拼路径"和"按路径找"两套规则各写一份、改一处漏一处。
 */
public final class UploadObjectKey {

    /** 所有上传对象的根前缀 */
    public static final String ROOT = "images/";

    /** 匿名上传（拿不到上传人ID时）的路径段 */
    public static final String ANONYMOUS = "anonymous";

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private UploadObjectKey() {
    }

    /**
     * 拼一个对象键
     *
     * @param ownerId 上传人ID，允许为 null（匿名）
     * @param suffix  扩展名（含点，小写），由调用方校验过
     * @param content 文件内容，用于算内容哈希
     * @param day     归属日期，一般传当天；显式传入便于测试与补跑历史数据
     */
    public static String of(Long ownerId, String suffix, byte[] content, LocalDate day) {
        String owner = ownerId == null ? ANONYMOUS : String.valueOf(ownerId);
        return dayPrefix(day) + owner + "/" + DigestUtil.md5Hex(content) + suffix;
    }

    /**
     * 某一天所有对象的前缀，形如 {@code images/2026/10/12/}
     * <p>
     * 清理任务用它圈定「这一天上传的图片」。
     */
    public static String dayPrefix(LocalDate day) {
        return ROOT + DAY.format(day) + "/";
    }

    /**
     * 从一个对象键里取出它归属的日期段（形如 {@code 2026/10/12}）
     *
     * @return 日期段；键不符合本布局（历史对象、外部写入）时返回空
     */
    public static Optional<String> dayOf(String objectKey) {
        if (objectKey == null || !objectKey.startsWith(ROOT)) {
            return Optional.empty();
        }
        // images/yyyy/MM/dd/... → 跳过 ROOT 后取前三段
        String[] segments = objectKey.substring(ROOT.length()).split("/");
        return segments.length >= 3 ? Optional.of(segments[0] + "/" + segments[1] + "/" + segments[2]) : Optional.empty();
    }
}
