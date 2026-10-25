package com.forum.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
@Data
public class LoginRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名过长")
    private String username;
    @NotBlank(message = "密码不能为空")
    @Size(max = 40, message = "密码过长")
    private String password;
}
