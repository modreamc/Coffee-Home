package com.forum.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data
@TableName("posts")
public class Post {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String content;
    private String category;
    private String images;
    private String links;
    private Long userId;
    private String authorName;
    private Integer likeCount = 0;
    private Integer collectCount = 0;
    private Integer commentCount = 0;
    private Boolean isTop = false;
    private Boolean isEssence = false;
    /** 标签，JSON 数组字符串，如 ["原神","攻略"] */
    private String tags;
    /** 视频链接，JSON 数组字符串，如 ["https://www.bilibili.com/video/BV1xx411c7mD"] */
    private String videos;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
