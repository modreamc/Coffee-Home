package com.forum.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data
@TableName("comments")
public class Comment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String content;
    private Long postId;
    private Long userId;
    private String userName;
    private Long parentId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
