package com.qk.service;

import java.time.LocalDate;

/**
 * 上传对象兜底回收
 * <p>
 * 同步删除（业务删除 / 换图时调用 {@link UploadService#releaseImage}）是尽力而为的，
 * 而且覆盖不了「上传了头像却取消新增」这种根本没有业务写入的场景。
 * 本服务按固定周期扫描台账，把<b>超过宽限期且不再被任何业务数据引用</b>的对象删掉。
 */
public interface UploadCleanupService {

    /**
     * 扫描并回收超期且无人引用的对象
     *
     * @return 本轮实际回收（删除对象存储文件）的数量
     */
    int cleanupOrphans();

    /**
     * 只处理「某一天上传的」对象
     * <p>
     * 对象键按 {@code images/年/月/日/上传人/内容哈希} 组织，因此按日期前缀就能精确圈定一天的范围：
     * 用于漏跑后精确补跑某一天、或按天对账。
     * <p>
     * <b>自动路径不走这里</b>：定时任务用的是 {@link #cleanupOrphans()}（宽限期 24 小时）。
     * 原因是「只处理昨天目录」会有安全缺口——昨晚 23:00 上传、今早 09:00 才提交表单的图片，
     * 会被凌晨的任务当成无效图片删掉，头像直接挂掉。按天入口留给人工补跑，范围明确且可重复执行。
     *
     * @param day 归属日期
     * @return 本轮实际回收的数量
     */
    int cleanupDay(LocalDate day);
}
