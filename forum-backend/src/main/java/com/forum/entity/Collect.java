package com.forum.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data
@TableName("collects")
public class Collect {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long postId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
