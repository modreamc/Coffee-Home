package com.forum.service;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.forum.dto.CommentRequest;
import com.forum.dto.PageResult;
import com.forum.entity.Comment;
import com.forum.entity.Post;
import com.forum.entity.User;
import com.forum.mapper.CommentMapper;
import com.forum.mapper.PostMapper;
import com.forum.mapper.UserMapper;
import com.forum.util.Paging;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
@Service
public class CommentService {
    /** 楼中楼层数上限，防止异常数据导致死循环。 */
    private static final int MAX_REPLY_DEPTH = 8;
    private final CommentMapper commentMapper;
    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    public CommentService(CommentMapper commentMapper, PostMapper postMapper, UserMapper userMapper,
                          NotificationService notificationService) {
        this.commentMapper = commentMapper;
        this.postMapper = postMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
    }
    @Transactional
    public Comment createComment(Long postId, CommentRequest request, String username) {
        Post post = postMapper.selectById(postId);
        if (post == null) throw new RuntimeException("帖子不存在");
        User user = requireUser(username);
        Comment parent = null;
        if (request.getParentId() != null) {
            parent = commentMapper.selectById(request.getParentId());
            // 防止把回复挂到别的帖子下面
            if (parent == null || !postId.equals(parent.getPostId())) {
                throw new RuntimeException("回复的评论不存在");
            }
        }
        Comment comment = new Comment();
        comment.setContent(request.getContent().trim());
        comment.setPostId(postId);
        comment.setUserId(user.getId());
        comment.setUserName(displayName(user));
        comment.setParentId(request.getParentId());
        commentMapper.insert(comment);
        postMapper.addCommentCount(postId, 1);
        notifyComment(post, parent, user);
        return comment;
    }
    /**
     * 主评论分页 + 该页主评论下的全部子孙回复一次性带回。
     * 这样前端按 parentId 组装楼中楼时，跨页的回复不会再凭空消失。
     */
    public PageResult<Comment> getComments(Long postId, int page, int size) {
        int safePage = Paging.page(page);
        int safeSize = Paging.size(size);
        Page<Comment> rootPage = new Page<>(safePage, safeSize);
        commentMapper.selectPage(rootPage, Wrappers.<Comment>lambdaQuery()
                .eq(Comment::getPostId, postId)
                .isNull(Comment::getParentId)
                .orderByDesc(Comment::getCreatedAt));
        List<Comment> roots = rootPage.getRecords();
        List<Comment> all = new ArrayList<>(roots);
        if (!roots.isEmpty()) {
            Set<Long> seen = new HashSet<>();
            roots.forEach(c -> seen.add(c.getId()));
            List<Long> frontier = roots.stream().map(Comment::getId).toList();
            for (int depth = 0; depth < MAX_REPLY_DEPTH && !frontier.isEmpty(); depth++) {
                List<Comment> children = commentMapper.selectList(Wrappers.<Comment>lambdaQuery()
                        .eq(Comment::getPostId, postId)
                        .in(Comment::getParentId, frontier)
                        .orderByAsc(Comment::getCreatedAt));
                children.removeIf(c -> !seen.add(c.getId()));
                if (children.isEmpty()) break;
                all.addAll(children);
                frontier = children.stream().map(Comment::getId).toList();
            }
        }
        return PageResult.of(all, rootPage.getTotal(), safePage, safeSize);
    }
    @Transactional
    public void deleteComment(Long commentId, String username) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) throw new RuntimeException("评论不存在");
        User user = requireUser(username);
        if (!comment.getUserId().equals(user.getId()) && !"ADMIN".equals(user.getRole())) {
            throw new RuntimeException("无权删除此评论");
        }
        List<Long> ids = collectDescendantIds(comment);
        ids.add(comment.getId());
        commentMapper.deleteBatchIds(ids);
        postMapper.addCommentCount(comment.getPostId(), -ids.size());
    }
    /** 删除某个用户的全部评论（含其楼中楼子孙），供管理员删除用户时级联清理。 */
    @Transactional
    public int deleteCommentsByUser(Long userId) {
        List<Comment> own = commentMapper.selectList(Wrappers.<Comment>lambdaQuery()
                .eq(Comment::getUserId, userId));
        if (own.isEmpty()) return 0;
        Set<Long> ids = new java.util.LinkedHashSet<>();
        for (Comment c : own) {
            ids.add(c.getId());
            ids.addAll(collectDescendantIds(c));
        }
        java.util.Map<Long, Long> perPost = commentMapper.selectBatchIds(ids).stream()
                .collect(java.util.stream.Collectors.groupingBy(Comment::getPostId,
                        java.util.stream.Collectors.counting()));
        commentMapper.deleteBatchIds(ids);
        perPost.forEach((postId, count) -> postMapper.addCommentCount(postId, -count.intValue()));
        return ids.size();
    }
    /** 递归收集子回复，避免删父评论后留下永远看不到的孤儿。 */
    private List<Long> collectDescendantIds(Comment root) {
        List<Long> result = new ArrayList<>();
        List<Long> frontier = List.of(root.getId());
        for (int depth = 0; depth < MAX_REPLY_DEPTH && !frontier.isEmpty(); depth++) {
            List<Comment> children = commentMapper.selectList(Wrappers.<Comment>lambdaQuery()
                    .eq(Comment::getPostId, root.getPostId())
                    .in(Comment::getParentId, frontier));
            if (children.isEmpty()) break;
            children.forEach(c -> result.add(c.getId()));
            frontier = children.stream().map(Comment::getId).toList();
        }
        return result;
    }
    private void notifyComment(Post post, Comment parent, User user) {
        String title = shortTitle(post.getTitle());
        if (post.getUserId() != null && !post.getUserId().equals(user.getId())) {
            notificationService.notify(post.getUserId(), "COMMENT",
                    displayName(user) + " 回复了你的帖子《" + title + "》", post.getId());
        }
        if (parent != null && parent.getUserId() != null
                && !parent.getUserId().equals(user.getId())
                && !parent.getUserId().equals(post.getUserId())) {
            notificationService.notify(parent.getUserId(), "COMMENT",
                    displayName(user) + " 回复了你在《" + title + "》下的评论", post.getId());
        }
    }
    private String displayName(User user) {
        String nickname = user.getNickname();
        return nickname == null || nickname.isBlank() ? user.getUsername() : nickname;
    }
    private String shortTitle(String title) {
        if (title == null) return "";
        return title.length() > 20 ? title.substring(0, 20) + "…" : title;
    }
    private User requireUser(String username) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, username));
        if (user == null) throw new RuntimeException("用户不存在");
        return user;
    }
}
