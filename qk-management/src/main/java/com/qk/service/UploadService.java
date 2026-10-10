package com.qk.service;

import java.io.InputStream;

/**
 * 图片上传
 * <p>
 * 上传策略（允许的格式、文件头校验、交给谁存储）属于业务规则，因此收在服务层；
 * 控制器只负责把 multipart 请求拆成「文件名 + 内容流」。
 * <p>
 * 文件大小仍由传输层限制（{@code spring.servlet.multipart.max-file-size}，超限转成「上传文件过大」），
 * 这里不再定义第二份上限，避免两处口径不一致。
 * <p>
 * 上传与「业务数据引用」是两个请求（前端先 {@code POST /upload} 拿地址，再提交表单），
 * 中间取消就会留下没人引用的对象。因此每次上传都会登记到 {@code upload_file} 台账，
 * 业务写入时用 {@link #bindImage} 绑定，业务删除/换图时用 {@link #releaseImage} 尽力同步删除，
 * 定时任务再用 {@link #recycleIfUnreferenced} 兜底回收。
 */
public interface UploadService {

    /** 业务类型：用户头像（台账 ref_type 的取值） */
    String REF_TYPE_USER = "user";

    /**
     * 校验并保存图片
     *
     * @param originalFilename 原始文件名，仅用于取扩展名与对象命名
     * @param content          文件内容流
     * @return 可直接访问的图片地址
     */
    String upload(String originalFilename, InputStream content);

    /**
     * 绑定：把上传得到的地址关联到具体业务数据
     * <p>
     * 由业务写入方在<b>自己的事务内</b>调用，业务回滚则绑定一起回滚，台账退回「临时」由回收兜底。
     * 地址不在台账中（外链、历史数据）或绑定失败都不影响业务写入，只记日志。
     *
     * @param url     上传返回的地址
     * @param refType 业务类型，见 {@link #REF_TYPE_USER}
     * @param refId   业务主键
     */
    void bindImage(String url, String refType, Long refId);

    /**
     * 释放：业务数据删除或换图后，尽力而为地同步删除不再被引用的对象
     * <p>
     * 在事务内调用时会推迟到<b>提交之后</b>执行：事务回滚则什么都不做，避免「业务没删成、图片先没了」。
     * 任何失败都只记日志、不抛出，绝不因为对象存储抖动让业务请求失败；漏掉的交给定时任务回收。
     *
     * @param url 不再使用的图片地址；为空或不属于本系统时不做任何事
     */
    void releaseImage(String url);

    /**
     * 若该地址对应的对象已不再被任何业务数据引用，则删除对象存储里的文件并标记台账
     * <p>
     * 回收的最终依据是「当前是否仍被引用」，而不是台账状态：状态可能因为绑定失败而过期，
     * 只有引用扫描才是真相。定时任务与同步释放共用这一个入口。
     *
     * @param url 图片地址
     * @return 是否真的删除了对象（台账中没有、仍被引用、已被其它线程回收、删除失败都返回 false）
     */
    boolean recycleIfUnreferenced(String url);
}
