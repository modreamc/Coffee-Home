package com.forum.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data
@TableName("announcements")
public class Announcement {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String content;
    private Boolean isActive = true;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
