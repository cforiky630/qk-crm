package com.qk.service;

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
}
