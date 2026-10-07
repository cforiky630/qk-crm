package com.qk.controller;

import com.qk.common.Result;
import com.qk.common.exception.BusinessException;
import com.qk.common.exception.ErrorCode;
import com.qk.entity.enums.Permission;
import com.qk.interceptor.RequirePermission;
import com.qk.service.UploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 文件上传控制器
 * <p>
 * 只做协议适配：把 multipart 请求拆成「文件名 + 内容流」交给 {@code UploadService}。
 * 上传策略（格式白名单、文件头校验）与存储实现都不在 Web 层，
 * 因此换存储或调整校验规则都不需要动控制器。
 */
@RestController
public class UploadController {

    private final UploadService uploadService;

    @Autowired
    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    /**
     * 文件上传
     *
     * @param image 上传的图片
     * @return 文件上传的 url
     */
    @RequirePermission(Permission.FILE_UPLOAD)
    @PostMapping("/upload")
    public Result<String> upload(MultipartFile image) throws IOException {
        // 没带文件分片时 Spring 解析出来的是 null（不是抛异常），必须在入口拦掉，
        // 否则 NPE 会被兜底处理器变成 500 + 运维告警。提示语与改造前一致。
        if (image == null) {
            throw new BusinessException(ErrorCode.UPLOAD_IMAGE_REQUIRED);
        }
        return Result.success(uploadService.upload(image.getOriginalFilename(), image.getInputStream()));
    }
}
