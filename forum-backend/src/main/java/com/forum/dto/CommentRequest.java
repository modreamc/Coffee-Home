package com.forum.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
@Data
public class CommentRequest {
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 5000, message = "评论最多 5000 字")
    private String content;
    private Long parentId;
}
