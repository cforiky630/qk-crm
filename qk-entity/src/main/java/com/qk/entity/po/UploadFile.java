package com.qk.entity.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 上传文件台账
 * 对应数据库表 upload_file
 * <p>
 * 每一次成功上传都会登记一条记录，是「临时对象回收」与「删除业务数据时同步删对象」的依据。
 * 对象名按内容寻址（同一用户 + 相同内容 = 同一对象），因此 {@code object_key} 上有唯一索引：
 * 一个对象只对应一条台账，回收时不存在「删一条误伤另一条」。
 */
@Data
@TableName("upload_file")
public class UploadFile {

    /** 主键 */
    @TableId
    private Long id;

    /** 对象存储里的键，如 images/12/<md5>.png */
    private String objectKey;

    /** 对外访问地址（写进业务表的那个值） */
    private String url;

    /** 上传人ID，取自上而下传递的当前登录用户 */
    private Long uploaderId;

    /** 内容 MD5，内容寻址命名的依据 */
    private String contentMd5;

    /** 字节数 */
    private Integer size;

    /** MIME 类型 */
    private String contentType;

    /** 状态，取值见 com.qk.entity.enums.UploadStatus：0 临时 / 1 已绑定 / 2 已回收 */
    private Integer status;

    /** 被引用时的业务类型，如 user */
    private String refType;

    /** 被引用时的业务主键 */
    private Long refId;

    /** 绑定（业务数据引用该对象）的时间 */
    private LocalDateTime bindTime;

    /** 回收尝试次数，连续失败说明对象存储或配置有问题 */
    private Integer retryCount;

    /** 上传时间（重复上传同一对象时刷新为本次上传时间，宽限期从本次重新计算） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 修改时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
