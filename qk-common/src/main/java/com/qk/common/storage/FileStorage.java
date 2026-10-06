package com.qk.common.storage;

/**
 * 文件存储出口（端口）
 * <p>
 * 服务层只依赖这个接口，不依赖阿里云 OSS 的 SDK 类型：换存储（本地磁盘、MinIO、S3）
 * 只要再提供一个实现即可，上传策略与接口契约都不用动；
 * 单元测试也可以用内存实现替掉真实对象存储。
 * <p>
 * 当前实现是 {@code com.qk.common.util.OssTemplate}。
 */
public interface FileStorage {

    /**
     * 保存文件并返回可直接访问的地址
     *
     * @param ownerId          上传人ID，用于按用户隔离对象路径；允许为 null（匿名）
     * @param originalFilename 原始文件名，仅用于取扩展名
     * @param content          文件内容
     * @return 可直接访问的 https 地址
     */
    String store(Long ownerId, String originalFilename, byte[] content);
}
