package com.forum.controller;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.forum.config.SecurityUtils;
import com.forum.dto.AdminUserView;
import com.forum.dto.ApiResponse;
import com.forum.entity.Announcement;
import com.forum.entity.Post;
import com.forum.entity.Tag;
import com.forum.mapper.AnnouncementMapper;
import com.forum.mapper.PostMapper;
import com.forum.mapper.TagMapper;
import com.forum.mapper.UserMapper;
import com.forum.service.CounterService;
import com.forum.service.HtmlSanitizer;
import com.forum.service.PostService;
import com.forum.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final UserMapper userMapper;
    private final PostMapper postMapper;
    private final TagMapper tagMapper;
    private final AnnouncementMapper announcementMapper;
    private final PostService postService;
    private final UserService userService;
    private final CounterService counterService;
    private final HtmlSanitizer htmlSanitizer;
    public AdminController(UserMapper userMapper, PostMapper postMapper, TagMapper tagMapper,
                           AnnouncementMapper announcementMapper, PostService postService,
                           UserService userService, CounterService counterService,
                           HtmlSanitizer htmlSanitizer) {
        this.userMapper = userMapper;
        this.postMapper = postMapper;
        this.tagMapper = tagMapper;
        this.announcementMapper = announcementMapper;
        this.postService = postService;
        this.userService = userService;
        this.counterService = counterService;
        this.htmlSanitizer = htmlSanitizer;
    }

    /** 管理端用户列表：只返回白名单字段，绝不带 password 哈希。 */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<AdminUserView>>> users() {
        List<AdminUserView> views = userMapper.selectList(
                        Wrappers.<com.forum.entity.User>lambdaQuery()
                                .orderByDesc(com.forum.entity.User::getCreatedAt))
                .stream().map(AdminUserView::from).toList();
        return ResponseEntity.ok(ApiResponse.success(views));
    }

    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<List<Post>>> posts() {
        return ResponseEntity.ok(ApiResponse.success(postMapper.selectList(
                Wrappers.<Post>lambdaQuery().orderByDesc(Post::getCreatedAt))));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Object>> stats() {
        long userCount = userMapper.selectCount(null);
        long postCount = postMapper.selectCount(null);
        long gameCount = postMapper.selectCount(Wrappers.<Post>lambdaQuery().eq(Post::getCategory, "GAME"));
        long animeCount = postMapper.selectCount(Wrappers.<Post>lambdaQuery().eq(Post::getCategory, "ANIME"));
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "userCount", userCount,
            "postCount", postCount,
            "gameCount", gameCount,
            "animeCount", animeCount
        )));
    }

    /** 计数对账：按真实记录重算点赞/收藏/评论/话题计数。 */
    @PostMapping("/stats/rebuild")
    public ResponseEntity<ApiResponse<Object>> rebuildCounters() {
        return ResponseEntity.ok(ApiResponse.success("已重算", counterService.rebuild()));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long userId, Authentication auth) {
        userService.deleteUser(userId, SecurityUtils.currentUsername());
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable Long postId, Authentication auth) {
        postService.deletePost(postId, SecurityUtils.currentUsername());
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    @PutMapping("/posts/{postId}/top")
    public ResponseEntity<ApiResponse<Void>> setTop(@PathVariable Long postId, @RequestBody Map<String, Boolean> body) {
        Post post = requirePost(postId);
        post.setIsTop(Boolean.TRUE.equals(body.get("top")));
        postMapper.updateById(post);
        return ResponseEntity.ok(ApiResponse.success("操作成功", null));
    }

    @PutMapping("/posts/{postId}/essence")
    public ResponseEntity<ApiResponse<Void>> setEssence(@PathVariable Long postId, @RequestBody Map<String, Boolean> body) {
        Post post = requirePost(postId);
        post.setIsEssence(Boolean.TRUE.equals(body.get("essence")));
        postMapper.updateById(post);
        return ResponseEntity.ok(ApiResponse.success("操作成功", null));
    }

    @GetMapping("/tags")
    public ResponseEntity<ApiResponse<List<Tag>>> tags() {
        return ResponseEntity.ok(ApiResponse.success(tagMapper.selectList(
                Wrappers.<Tag>lambdaQuery().orderByDesc(Tag::getPostCount))));
    }

    /** 删除标签时同步把它从帖子的 tags JSON 里摘掉，避免"帖子还挂着不存在的标签"。 */
    @DeleteMapping("/tags/{tagId}")
    public ResponseEntity<ApiResponse<Void>> deleteTag(@PathVariable Long tagId) {
        Tag tag = tagMapper.selectById(tagId);
        if (tag == null) throw new RuntimeException("标签不存在");
        postService.removeTagFromAllPosts(tag.getName());
        tagMapper.deleteById(tagId);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    @GetMapping("/announcements")
    public ResponseEntity<ApiResponse<List<Announcement>>> announcements() {
        return ResponseEntity.ok(ApiResponse.success(announcementMapper.selectList(
                Wrappers.<Announcement>lambdaQuery().orderByDesc(Announcement::getCreatedAt))));
    }

    @PostMapping("/announcements")
    public ResponseEntity<ApiResponse<Announcement>> createAnnouncement(@RequestBody Map<String, String> body) {
        String title = body.get("title");
        String content = body.get("content");
        if (title == null || title.isBlank()) throw new RuntimeException("公告标题不能为空");
        if (title.length() > 100) throw new RuntimeException("公告标题最多 100 字");
        if (content != null && content.length() > 2000) throw new RuntimeException("公告内容最多 2000 字");
        Announcement a = new Announcement();
        a.setTitle(htmlSanitizer.toPlainText(title).trim());
        a.setContent(htmlSanitizer.toPlainText(content));
        a.setIsActive(true);
        announcementMapper.insert(a);
        return ResponseEntity.ok(ApiResponse.success("发布成功", a));
    }

    @DeleteMapping("/announcements/{annId}")
    public ResponseEntity<ApiResponse<Void>> deleteAnnouncement(@PathVariable Long annId) {
        announcementMapper.deleteById(annId);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    private Post requirePost(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null) throw new RuntimeException("帖子不存在");
        return post;
    }
}
