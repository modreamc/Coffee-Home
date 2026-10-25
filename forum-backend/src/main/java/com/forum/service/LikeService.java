package com.forum.service;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.forum.entity.Like;
import com.forum.entity.Post;
import com.forum.entity.User;
import com.forum.mapper.LikeMapper;
import com.forum.mapper.PostMapper;
import com.forum.mapper.UserMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.LinkedHashMap;
import java.util.Map;
@Service
public class LikeService {
    private final LikeMapper likeMapper;
    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    public LikeService(LikeMapper likeMapper, PostMapper postMapper, UserMapper userMapper,
                       NotificationService notificationService) {
        this.likeMapper = likeMapper;
        this.postMapper = postMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
    }
    @Transactional
    public Map<String, Object> toggleLike(Long postId, String username) {
        User user = requireUser(username);
        Post post = postMapper.selectById(postId);
        if (post == null) throw new RuntimeException("帖子不存在");
        int removed = likeMapper.delete(Wrappers.<Like>lambdaQuery()
                .eq(Like::getUserId, user.getId())
                .eq(Like::getPostId, postId));
        if (removed > 0) {
            postMapper.addLikeCount(postId, -1);
            return result(false, postId);
        }
        Like like = new Like();
        like.setUserId(user.getId());
        like.setPostId(postId);
        try {
            likeMapper.insert(like);
        } catch (DuplicateKeyException e) {
            // 并发重复点赞：唯一键兜底，不做二次计数
            return result(true, postId);
        }
        postMapper.addLikeCount(postId, 1);
        if (post.getUserId() != null && !post.getUserId().equals(user.getId())) {
            notificationService.notify(post.getUserId(), "LIKE",
                    user.getNickname() + " 赞了你的帖子《" + shortTitle(post.getTitle()) + "》", postId);
        }
        return result(true, postId);
    }
    public boolean isLiked(Long postId, String username) {
        if (username == null) return false;
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, username));
        if (user == null) return false;
        return likeMapper.selectCount(Wrappers.<Like>lambdaQuery()
                .eq(Like::getUserId, user.getId())
                .eq(Like::getPostId, postId)) > 0;
    }
    private Map<String, Object> result(boolean liked, Long postId) {
        Post fresh = postMapper.selectById(postId);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("liked", liked);
        body.put("likeCount", fresh == null || fresh.getLikeCount() == null ? 0 : fresh.getLikeCount());
        return body;
    }
    private User requireUser(String username) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, username));
        if (user == null) throw new RuntimeException("用户不存在");
        return user;
    }
    private String shortTitle(String title) {
        if (title == null) return "";
        return title.length() > 20 ? title.substring(0, 20) + "…" : title;
    }
}
