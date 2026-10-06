package com.qk.controller;

import com.qk.common.Result;
import com.qk.common.exception.BusinessException;
import com.qk.common.util.OssTemplate;
import com.qk.common.util.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
public class UploadController {

    /** 允许上传的图片格式，避免把可执行文件之类的内容塞进图片目录 */
    private static final List<String> ALLOWED_SUFFIX = List.of(".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp");

    private final OssTemplate ossTemplate;

    @Autowired
    public UploadController(OssTemplate ossTemplate) {
        this.ossTemplate = ossTemplate;
    }

    /**
     * 文件上传
     *
     * @param image 上传的图片
     * @return 文件上传的 url
     */
    @PostMapping("/upload")
    public Result<String> upload(MultipartFile image) throws IOException {
        if (image == null || image.isEmpty()) {
            throw new BusinessException("请选择要上传的图片");
        }
        String originalFilename = image.getOriginalFilename();
        String suffix = originalFilename == null ? ""
                : originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        if (!ALLOWED_SUFFIX.contains(suffix)) {
            throw new BusinessException("只支持 jpg、jpeg、png、gif、bmp、webp 格式的图片");
        }

        // 对象名带上上传人，便于后续按用户追溯与清理孤儿对象
        Long userId = UserHolder.getCurrentUser();
        log.info("文件上传开始：{}，上传人：{}", originalFilename, userId);
        String url = ossTemplate.upload(userId, originalFilename, image.getInputStream());
        log.info("文件上传完成：{}", url);
        return Result.success(url);
    }
}
