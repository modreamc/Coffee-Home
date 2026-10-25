package com.forum.controller;
import com.forum.config.SecurityUtils;
import com.forum.dto.ApiResponse;
import com.forum.dto.PageResult;
import com.forum.entity.Post;
import com.forum.service.CollectService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController
@RequestMapping("/api/collects")
public class CollectController {
    private final CollectService collectService;
    public CollectController(CollectService collectService) { this.collectService = collectService; }

    @PostMapping("/{postId}")
    public ResponseEntity<ApiResponse<Object>> toggle(@PathVariable Long postId, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                collectService.toggleCollect(postId, requireUsername(auth))));
    }

    @GetMapping("/{postId}/status")
    public ResponseEntity<ApiResponse<Object>> status(@PathVariable Long postId, Authentication auth) {
        String username = SecurityUtils.usernameOrNull(auth);
        boolean collected = username != null && collectService.isCollected(postId, username);
        return ResponseEntity.ok(ApiResponse.success(Map.of("collected", collected)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<Post>>> list(
            Authentication auth,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                collectService.getUserCollects(requireUsername(auth), page, size)));
    }

    private String requireUsername(Authentication auth) {
        String username = SecurityUtils.usernameOrNull(auth);
        if (username == null) throw new RuntimeException("未登录");
        return username;
    }
}
