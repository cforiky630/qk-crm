package com.qk.entity.enums;

import lombok.Getter;

/**
 * 上传对象在台账里的状态
 * <p>
 * 状态只用于记账与观测，<b>不是</b>回收的最终依据：真正决定「能不能删」的是
 * 「该对象当前是否仍被业务数据引用」。这样即使绑定那一步失败或漏掉，
 * 也不会误删仍在使用的图片。
 */
@Getter
public enum UploadStatus implements CodeEnum<Integer> {

    /** 临时：已上传但还没有被任何业务数据引用（例如上传了头像却取消了新增） */
    TEMP(0),

    /** 已绑定：已经被某条业务数据引用（如某个用户的头像） */
    BOUND(1),

    /** 已回收：对象存储里的文件已经删除，台账不再处理 */
    RECYCLED(2);

    /** 码值：数据库与对外都用这个数字 */
    private final Integer value;

    UploadStatus(Integer value) {
        this.value = value;
    }
}
