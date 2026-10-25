package com.forum.controller;
import com.forum.config.SecurityUtils;
import com.forum.dto.*;
import com.forum.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;
    public AuthController(UserService userService) { this.userService = userService; }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Object>> login(@Valid @RequestBody LoginRequest request,
                                                     HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success(userService.login(request, clientKey(httpRequest))));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Object>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(ApiResponse.success("注册成功", userService.register(request)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserInfo>> me(Authentication auth) {
        String username = SecurityUtils.usernameOrNull(auth);
        if (username == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "未登录"));
        }
        return ResponseEntity.ok(ApiResponse.success(userService.getCurrentUser(username)));
    }

    private String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
