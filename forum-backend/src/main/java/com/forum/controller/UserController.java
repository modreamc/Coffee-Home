package com.forum.controller;
import com.forum.dto.ApiResponse;
import com.forum.entity.User;
import com.forum.mapper.UserMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserMapper userMapper;
    public UserController(UserMapper userMapper) { this.userMapper = userMapper; }
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> info(@PathVariable Long id) {
        User user = userMapper.selectById(id);
        if (user == null) throw new RuntimeException("用户不存在");
        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("nickname", user.getNickname());
        info.put("avatar", user.getAvatar());
        info.put("role", user.getRole());
        info.put("createdAt", user.getCreatedAt());
        int points = user.getPoints() != null ? user.getPoints() : 0;
        info.put("points", points);
        info.put("level", com.forum.service.UserService.levelName(points));
        return ResponseEntity.ok(ApiResponse.success(info));
    }
}
