package com.forum.dto;
import com.forum.entity.User;
import lombok.Data;
import java.time.LocalDateTime;
/**
 * 管理端用户视图：绝不包含 password 等敏感字段。
 */
@Data
public class AdminUserView {
    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String avatar;
    private String role;
    private Integer points;
    private LocalDateTime createdAt;

    public static AdminUserView from(User user) {
        AdminUserView view = new AdminUserView();
        view.id = user.getId();
        view.username = user.getUsername();
        view.nickname = user.getNickname();
        view.email = user.getEmail();
        view.avatar = user.getAvatar();
        view.role = user.getRole();
        view.points = user.getPoints() == null ? 0 : user.getPoints();
        view.createdAt = user.getCreatedAt();
        return view;
    }
}
