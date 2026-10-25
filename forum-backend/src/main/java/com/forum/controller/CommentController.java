package com.forum.controller;
import com.forum.config.SecurityUtils;
import com.forum.dto.*;
import com.forum.entity.Comment;
import com.forum.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/comments")
public class CommentController {
    private final CommentService commentService;
    public CommentController(CommentService commentService) { this.commentService = commentService; }

    @PostMapping("/{postId}")
    public ResponseEntity<ApiResponse<Comment>> create(@PathVariable Long postId,
                                                       @Valid @RequestBody CommentRequest request,
                                                       Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success("评论成功",
                commentService.createComment(postId, request, requireUsername(auth))));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PageResult<Comment>>> list(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(commentService.getComments(postId, page, size)));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long commentId, Authentication auth) {
        commentService.deleteComment(commentId, requireUsername(auth));
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    private String requireUsername(Authentication auth) {
        String username = SecurityUtils.usernameOrNull(auth);
        if (username == null) throw new RuntimeException("未登录");
        return username;
    }
}
