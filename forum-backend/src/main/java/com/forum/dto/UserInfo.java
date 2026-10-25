package com.forum.dto;
import lombok.Data;
@Data
public class UserInfo {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String email;
    private String role;
    private Integer points = 0;
    private String level;
}
