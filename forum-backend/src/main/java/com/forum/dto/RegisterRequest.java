package com.forum.dto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
@Data
public class RegisterRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度3-20位")
    @Pattern(regexp = "^[A-Za-z0-9_\\u4e00-\\u9fa5]+$", message = "用户名只能是字母、数字、下划线或中文")
    private String username;
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 40, message = "密码长度6-40位")
    private String password;
    @Size(max = 50, message = "昵称最多 50 字")
    private String nickname;
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱最多 100 字")
    private String email;
}
