package com.qk.common.util;

/**
 * 文件名工具
 * <p>
 * 上传链路里有两处需要从文件名取扩展名（控制器做格式白名单校验、OSS 对象名拼接），
 * 取法必须一致，且都不能在「没有扩展名」时炸掉 —— 返回空串，由调用方自己决定怎么处理。
 */
public final class FileNameUtil {

    private FileNameUtil() {
    }

    /**
     * 取扩展名（含点，保留原大小写）
     *
     * @param fileName 原始文件名，允许为 null
     * @return 形如 {@code .png} 的扩展名；没有扩展名或文件名为 null 时返回空串
     */
    public static String extensionOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex < 0 ? "" : fileName.substring(dotIndex);
    }
}
