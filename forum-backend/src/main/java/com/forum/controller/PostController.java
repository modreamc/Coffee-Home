package com.forum.controller;
import com.forum.config.SecurityUtils;
import com.forum.dto.*;
import com.forum.entity.Post;
import com.forum.service.PostService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/posts")
public class PostController {
    private final PostService postService;
    public PostController(PostService postService) { this.postService = postService; }

    @PostMapping
    public ResponseEntity<ApiResponse<Post>> create(@Valid @RequestBody PostRequest request, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success("发布成功",
                postService.createPost(request, requireUsername(auth))));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<Post>>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(postService.getPosts(category, tag, sort, page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Post>> detail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(postService.getPost(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Post>> update(@PathVariable Long id,
                                                    @Valid @RequestBody PostRequest request,
                                                    Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                postService.updatePost(id, request, requireUsername(auth))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id, Authentication auth) {
        postService.deletePost(id, requireUsername(auth));
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    /** 搜索改为分页返回，避免整表加载。 */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResult<Post>>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(postService.searchPosts(keyword, page, size)));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<PageResult<Post>>> userPosts(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(postService.getUserPosts(userId, page, size)));
    }

    private String requireUsername(Authentication auth) {
        String username = SecurityUtils.usernameOrNull(auth);
        if (username == null) throw new RuntimeException("未登录");
        return username;
    }
}
