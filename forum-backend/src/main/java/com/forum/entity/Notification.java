package com.forum.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data
@TableName("notifications")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String type;
    private String content;
    private Long relatedId;
    /**
     * 必须使用包装类型 Boolean：
     * Lombok 对基本类型 boolean isXxx 只生成 isXxx()/setXxx()，
     * 会与 MyBatis-Plus 的 getIsRead 列推导、Jackson 的 isRead 字段名冲突。
     */
    private Boolean isRead = false;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
