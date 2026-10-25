package com.forum.controller;
import com.forum.config.SecurityUtils;
import com.forum.dto.ApiResponse;
import com.forum.service.LikeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController
@RequestMapping("/api/likes")
public class LikeController {
    private final LikeService likeService;
    public LikeController(LikeService likeService) { this.likeService = likeService; }

    @PostMapping("/{postId}")
    public ResponseEntity<ApiResponse<Object>> toggle(@PathVariable Long postId, Authentication auth) {
        String username = SecurityUtils.usernameOrNull(auth);
        if (username == null) throw new RuntimeException("未登录");
        return ResponseEntity.ok(ApiResponse.success(likeService.toggleLike(postId, username)));
    }

    @GetMapping("/{postId}/status")
    public ResponseEntity<ApiResponse<Object>> status(@PathVariable Long postId, Authentication auth) {
        String username = SecurityUtils.usernameOrNull(auth);
        boolean liked = username != null && likeService.isLiked(postId, username);
        return ResponseEntity.ok(ApiResponse.success(Map.of("liked", liked)));
    }
}
