package com.forum.controller;

import com.forum.dto.ApiResponse;
import com.forum.dto.VideoMeta;
import com.forum.service.VideoMetaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 视频元信息：发帖页粘贴 B站 / YouTube 链接时，用它抓标题和封面做预览。
 */
@RestController
@RequestMapping("/api/videos")
public class VideoController {

    private final VideoMetaService videoMetaService;

    public VideoController(VideoMetaService videoMetaService) {
        this.videoMetaService = videoMetaService;
    }

    @GetMapping("/meta")
    public ResponseEntity<ApiResponse<VideoMeta>> meta(@RequestParam String url) {
        return ResponseEntity.ok(ApiResponse.success(videoMetaService.fetch(url)));
    }
}
