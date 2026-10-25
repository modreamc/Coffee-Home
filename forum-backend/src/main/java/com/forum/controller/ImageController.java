package com.forum.controller;
import com.forum.dto.ApiResponse;
import com.forum.service.ImageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;
@RestController
@RequestMapping("/api/images")
public class ImageController {
    private final ImageService imageService;
    public ImageController(ImageService imageService) { this.imageService = imageService; }
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Object>> upload(@RequestParam("file") MultipartFile file) {
        String url = imageService.saveImage(file);
        return ResponseEntity.ok(ApiResponse.success(Map.of("url", url)));
    }
}
