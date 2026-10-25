package com.forum.controller;
import com.forum.config.SecurityUtils;
import com.forum.dto.ApiResponse;
import com.forum.service.SignInService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController
@RequestMapping("/api/signin")
public class SignInController {
    private final SignInService signInService;
    public SignInController(SignInService signInService) { this.signInService = signInService; }

    @PostMapping
    public ResponseEntity<ApiResponse<Object>> signIn(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(signInService.signIn(requireUsername(auth))));
    }

    @GetMapping("/today")
    public ResponseEntity<ApiResponse<Object>> today(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("signed", signInService.isSignedToday(requireUsername(auth)))));
    }

    private String requireUsername(Authentication auth) {
        String username = SecurityUtils.usernameOrNull(auth);
        if (username == null) throw new RuntimeException("未登录");
        return username;
    }
}
