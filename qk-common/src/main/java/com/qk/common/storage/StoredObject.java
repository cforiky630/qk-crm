package com.qk.common.storage;

/**
 * 一次「保存文件」操作的结果
 * <p>
 * 改造前 {@link FileStorage#store} 只返回一个 URL 字符串，服务层拿不到对象键、内容摘要与大小，
 * 也就无法登记上传台账、无法在业务数据删除后回收对象。现在把存储侧知道的信息一次性带出来，
 * 上传策略与回收策略都不需要再自己解析 URL 反推对象键。
 *
 * @param url         可直接访问的 https 地址，即对外返回给前端、写进业务表的值
 * @param objectKey   对象存储里的键，回收时按它删除对象
 * @param contentMd5  内容 MD5（内容寻址命名的输入，也是「同一内容只有一份」的依据）
 * @param size        字节数
 * @param contentType 写入对象存储时使用的 MIME 类型
 */
public record StoredObject(String url, String objectKey, String contentMd5, int size, String contentType) {
}
