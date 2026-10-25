package com.forum.config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.nio.file.Path;
import java.nio.file.Paths;
@Configuration
public class WebConfig implements WebMvcConfigurer {
    private static final Logger log = LoggerFactory.getLogger(WebConfig.class);
    private final Path uploadRoot;
    public WebConfig(@Value("${app.upload-dir:./picture}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        log.info("image upload directory: {}", uploadRoot);
    }
    public Path getUploadRoot() { return uploadRoot; }
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 图片统一存放在 picture 文件夹，通过 /picture/** 访问
        String location = uploadRoot.toUri().toString();
        registry.addResourceHandler("/picture/**").addResourceLocations(location);
        // 兼容旧的 /uploads/** 引用（指向同一目录）
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }
}
