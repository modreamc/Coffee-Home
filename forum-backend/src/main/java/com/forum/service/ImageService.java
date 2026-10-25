package com.forum.service;
import com.forum.config.WebConfig;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
/**
 * 图片保存：只按文件头魔数判定类型，扩展名由服务端生成，
 * 不再信任客户端文件名 —— 避免上传 .html/.svg 造成同源 XSS，也避免扩展名拼接导致的目录穿越。
 */
@Service
public class ImageService {
    private static final Logger log = LoggerFactory.getLogger(ImageService.class);
    private static final long MAX_BYTES = 10L * 1024 * 1024;
    private final Path root;
    public ImageService(WebConfig webConfig) {
        this.root = webConfig.getUploadRoot();
    }
    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("无法创建上传目录: " + root, e);
        }
    }
    public String saveImage(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new RuntimeException("文件为空");
        if (file.getSize() > MAX_BYTES) throw new RuntimeException("图片不能超过 10MB");
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("图片读取失败");
        }
        String ext = detectExtension(bytes);
        if (ext == null) throw new RuntimeException("只支持 JPG / PNG / GIF / WEBP 格式的图片");
        String fileName = UUID.randomUUID() + "." + ext;
        Path target = root.resolve(fileName).normalize();
        if (!target.startsWith(root)) {
            throw new RuntimeException("非法的文件路径");
        }
        try {
            Files.write(target, bytes);
        } catch (IOException e) {
            log.error("save image failed: {}", target, e);
            throw new RuntimeException("图片上传失败");
        }
        return "/picture/" + fileName;
    }

    /**
     * 保存外部抓取的图片（如视频封面）：只按魔数判断类型，
     * 文件名使用调用方给出的安全前缀，避免覆盖/穿越。
     */
    public String saveRemoteImage(byte[] bytes, String namePrefix) {
        if (bytes == null || bytes.length < 5_000) return null;
        String ext = detectExtension(bytes);
        if (ext == null) return null;
        String safe = namePrefix == null ? UUID.randomUUID().toString()
                : namePrefix.replaceAll("[^A-Za-z0-9_-]", "");
        if (safe.isEmpty()) safe = UUID.randomUUID().toString();
        String fileName = safe + "." + ext;
        Path target = root.resolve(fileName).normalize();
        if (!target.startsWith(root)) return null;
        try {
            Files.write(target, bytes);
            return "/picture/" + fileName;
        } catch (IOException e) {
            log.warn("save remote image failed: {}", target, e);
            return null;
        }
    }

    private String detectExtension(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && (b[4] & 0xFF) == 0x0D && (b[5] & 0xFF) == 0x0A && (b[6] & 0xFF) == 0x1A && (b[7] & 0xFF) == 0x0A) {
            return "png";
        }
        if (b.length >= 6 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8'
                && (b[4] == '7' || b[4] == '9') && b[5] == 'a') {
            return "gif";
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "webp";
        }
        return null;
    }
}
