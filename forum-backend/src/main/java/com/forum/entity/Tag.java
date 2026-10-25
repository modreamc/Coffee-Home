package com.forum.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data
@TableName("tags")
public class Tag {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Integer postCount = 0;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
