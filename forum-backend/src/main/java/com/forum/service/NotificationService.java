package com.forum.service;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.forum.entity.Notification;
import com.forum.entity.User;
import com.forum.mapper.NotificationMapper;
import com.forum.mapper.UserMapper;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
@Service
public class NotificationService {
    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;
    private final HtmlSanitizer htmlSanitizer;
    public NotificationService(NotificationMapper notificationMapper, UserMapper userMapper,
                               HtmlSanitizer htmlSanitizer) {
        this.notificationMapper = notificationMapper;
        this.userMapper = userMapper;
        this.htmlSanitizer = htmlSanitizer;
    }
    public void notify(Long userId, String type, String content, Long relatedId) {
        if (userId == null) return;
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setContent(truncate(htmlSanitizer.toPlainText(content), 255));
        n.setRelatedId(relatedId);
        n.setIsRead(false);
        notificationMapper.insert(n);
    }
    public Map<String, Object> list(String username) {
        User user = requireUser(username);
        Long unread = notificationMapper.selectCount(Wrappers.<Notification>lambdaQuery()
                .eq(Notification::getUserId, user.getId())
                .eq(Notification::getIsRead, false));
        List<Notification> list = notificationMapper.selectList(Wrappers.<Notification>lambdaQuery()
                .eq(Notification::getUserId, user.getId())
                .orderByDesc(Notification::getCreatedAt)
                .last("LIMIT 30"));
        return Map.of("list", list, "unreadCount", unread);
    }
    public void markAllRead(String username) {
        User user = requireUser(username);
        notificationMapper.update(null, Wrappers.<Notification>lambdaUpdate()
                .eq(Notification::getUserId, user.getId())
                .eq(Notification::getIsRead, false)
                .set(Notification::getIsRead, true));
    }
    /** 帖子被删除时清理相关通知，避免通知点进去 404。 */
    public void deleteByRelatedPost(Long postId) {
        if (postId == null) return;
        notificationMapper.delete(Wrappers.<Notification>lambdaQuery()
                .eq(Notification::getRelatedId, postId));
    }
    public void deleteByUser(Long userId) {
        notificationMapper.delete(Wrappers.<Notification>lambdaQuery()
                .eq(Notification::getUserId, userId));
    }
    private User requireUser(String username) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, username));
        if (user == null) throw new RuntimeException("用户不存在");
        return user;
    }
    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) : s;
    }
}
