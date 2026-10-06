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
 */
public interface UploadService {

    /**
     * 校验并保存图片
     *
     * @param originalFilename 原始文件名，仅用于取扩展名与对象命名
     * @param content          文件内容流
     * @return 可直接访问的图片地址
     */
    String upload(String originalFilename, InputStream content);
}
