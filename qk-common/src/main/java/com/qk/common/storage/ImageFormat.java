package com.qk.common.storage;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 允许上传的图片格式
 * <p>
 * 改造前同一份格式清单散在两个地方：控制器里是扩展名白名单，OSS 模板里是
 * 「扩展名 → Content-Type」的表，新增一种格式要改两处、且可能只改一处。
 * 现在扩展名、Content-Type、文件头签名收敛到这一个枚举里。
 * <p>
 * {@link #matches(byte[])} 是「文件头（魔术字节）校验」：只校验扩展名时，
 * 把 {@code evil.sh} 改名成 {@code evil.png} 就能通过，对象存储里会因此出现可执行内容。
 */
public enum ImageFormat {

    /** JPEG：FF D8 FF */
    JPEG("image/jpeg", ".jpg", ".jpeg"),

    /** PNG：89 50 4E 47 0D 0A 1A 0A */
    PNG("image/png", ".png"),

    /** GIF：GIF87a / GIF89a */
    GIF("image/gif", ".gif"),

    /** BMP：BM */
    BMP("image/bmp", ".bmp"),

    /** WEBP：RIFF....WEBP */
    WEBP("image/webp", ".webp");

    private final String contentType;
    private final List<String> extensions;

    ImageFormat(String contentType, String... extensions) {
        this.contentType = contentType;
        this.extensions = List.of(extensions);
    }

    /** 对象存储使用的 Content-Type */
    public String contentType() {
        return contentType;
    }

    /** 该格式允许的扩展名（含点，小写） */
    public List<String> extensions() {
        return extensions;
    }

    /**
     * 按扩展名识别格式，大小写不敏感
     *
     * @param extension 形如 {@code .png} 的扩展名，允许为 null
     * @return 匹配到的格式；无扩展名或不在白名单时返回空
     */
    public static Optional<ImageFormat> ofExtension(String extension) {
        if (extension == null || extension.isEmpty()) {
            return Optional.empty();
        }
        String normalized = extension.toLowerCase(Locale.ROOT);
        for (ImageFormat format : values()) {
            if (format.extensions.contains(normalized)) {
                return Optional.of(format);
            }
        }
        return Optional.empty();
    }

    /**
     * 文件头是否与该格式匹配
     *
     * @param content 文件内容，长度不足时返回 false（不会越界）
     */
    public boolean matches(byte[] content) {
        return switch (this) {
            case JPEG -> startsWith(content, 0xFF, 0xD8, 0xFF);
            case PNG -> startsWith(content, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
            case GIF -> startsWith(content, 'G', 'I', 'F', '8', '7', 'a')
                    || startsWith(content, 'G', 'I', 'F', '8', '9', 'a');
            case BMP -> startsWith(content, 'B', 'M');
            // RIFF 容器的第 8~11 字节标识具体类型，WEBP 要求两段都匹配
            case WEBP -> startsWith(content, 'R', 'I', 'F', 'F')
                    && matchesAt(content, 8, 'W', 'E', 'B', 'P');
        };
    }

    private static boolean startsWith(byte[] content, int... signature) {
        return matchesAt(content, 0, signature);
    }

    private static boolean matchesAt(byte[] content, int offset, int... signature) {
        if (content == null || content.length < offset + signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((content[offset + i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }
}
