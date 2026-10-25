package com.forum.controller;
import com.forum.config.SecurityUtils;
import com.forum.dto.ApiResponse;
import com.forum.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Object>> list(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.list(requireUsername(auth))));
    }

    @PostMapping("/read")
    public ResponseEntity<ApiResponse<Void>> readAll(Authentication auth) {
        notificationService.markAllRead(requireUsername(auth));
        return ResponseEntity.ok(ApiResponse.success("已读", null));
    }

    private String requireUsername(Authentication auth) {
        String username = SecurityUtils.usernameOrNull(auth);
        if (username == null) throw new RuntimeException("未登录");
        return username;
    }
}
