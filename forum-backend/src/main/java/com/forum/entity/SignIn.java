package com.forum.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Data
@TableName("sign_ins")
public class SignIn {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private LocalDate signDate;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
