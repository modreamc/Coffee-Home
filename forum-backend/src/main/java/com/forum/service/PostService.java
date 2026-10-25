package com.forum.service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forum.config.WebConfig;
import com.forum.dto.PageResult;
import com.forum.dto.PostRequest;
import com.forum.entity.Comment;
import com.forum.entity.Like;
import com.forum.entity.Collect;
import com.forum.entity.Post;
import com.forum.entity.Tag;
import com.forum.entity.User;
import com.forum.mapper.CollectMapper;
import com.forum.mapper.CommentMapper;
import com.forum.mapper.LikeMapper;
import com.forum.mapper.NotificationMapper;
import com.forum.mapper.PostMapper;
import com.forum.mapper.TagMapper;
import com.forum.mapper.UserMapper;
import com.forum.util.Paging;
import com.forum.util.VideoLinks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
@Service
public class PostService {
    private static final Logger log = LoggerFactory.getLogger(PostService.class);
    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final TagMapper tagMapper;
    private final CommentMapper commentMapper;
    private final LikeMapper likeMapper;
    private final CollectMapper collectMapper;
    private final NotificationMapper notificationMapper;
    private final ObjectMapper objectMapper;
    private final HtmlSanitizer htmlSanitizer;
    private final VideoMetaService videoMetaService;
    private final Path uploadRoot;
    public PostService(PostMapper postMapper, UserMapper userMapper, TagMapper tagMapper,
                       CommentMapper commentMapper, LikeMapper likeMapper, CollectMapper collectMapper,
                       NotificationMapper notificationMapper, ObjectMapper objectMapper,
                       HtmlSanitizer htmlSanitizer, VideoMetaService videoMetaService,
                       WebConfig webConfig) {
        this.postMapper = postMapper;
        this.userMapper = userMapper;
        this.tagMapper = tagMapper;
        this.commentMapper = commentMapper;
        this.likeMapper = likeMapper;
        this.collectMapper = collectMapper;
        this.notificationMapper = notificationMapper;
        this.objectMapper = objectMapper;
        this.htmlSanitizer = htmlSanitizer;
        this.videoMetaService = videoMetaService;
        this.uploadRoot = webConfig.getUploadRoot();
    }
    @Transactional
    public Post createPost(PostRequest request, String username) {
        User user = requireUser(username);
        Post post = new Post();
        post.setTitle(htmlSanitizer.toPlainText(request.getTitle()));
        post.setContent(htmlSanitizer.sanitize(request.getContent()));
        post.setCategory(request.getCategory());
        post.setImages(normalizeImages(request.getImages()));
        post.setLinks(request.getLinks());
        post.setUserId(user.getId());
        post.setAuthorName(displayName(user));
        post.setTags(serializeTags(request.getTags()));
        post.setVideos(serializeVideos(request.getVideos()));
        post.setLikeCount(0);
        post.setCollectCount(0);
        post.setCommentCount(0);
        postMapper.insert(post);
        applyTagCounts(request.getTags(), 1);
        return post;
    }
    public PageResult<Post> getPosts(String category, String tag, String sort, int page, int size) {
        int safePage = Paging.page(page);
        int safeSize = Paging.size(size);
        Page<Post> postPage = new Page<>(safePage, safeSize);
        LambdaQueryWrapper<Post> query = Wrappers.<Post>lambdaQuery();
        if (category != null && !category.isEmpty()) {
            query.eq(Post::getCategory, category);
        }
        if (tag != null && !tag.isEmpty()) {
            // tags 存的是 JSON 数组字符串，按 "标签" 精确匹配；通配符已转义
            query.like(Post::getTags, "\"" + Paging.escapeLike(tag) + "\"");
        }
        if ("hot".equals(sort)) {
            // 热度 = 点赞*3 + 收藏*2 + 评论*4，按热度降序
            query.last("ORDER BY (like_count * 3 + collect_count * 2 + comment_count * 4) DESC, created_at DESC");
        } else if ("essence".equals(sort)) {
            query.eq(Post::getIsEssence, true).orderByDesc(Post::getCreatedAt);
        } else {
            query.orderByDesc(Post::getIsTop).orderByDesc(Post::getCreatedAt);
        }
        postMapper.selectPage(postPage, query);
        return PageResult.of(postPage.getRecords(), postPage.getTotal(), safePage, safeSize);
    }
    public Post getPost(Long id) {
        Post post = postMapper.selectById(id);
        if (post == null) throw new RuntimeException("帖子不存在");
        return post;
    }
    @Transactional
    public Post updatePost(Long id, PostRequest request, String username) {
        Post post = getPost(id);
        User user = requireUser(username);
        if (!isOwnerOrAdmin(post, user)) {
            throw new RuntimeException("无权修改此帖子");
        }
        List<String> oldTags = parseTags(post.getTags());
        post.setTitle(htmlSanitizer.toPlainText(request.getTitle()));
        post.setContent(htmlSanitizer.sanitize(request.getContent()));
        post.setCategory(request.getCategory());
        if (request.getImages() != null) post.setImages(normalizeImages(request.getImages()));
        if (request.getLinks() != null) post.setLinks(request.getLinks());
        List<String> newTags = request.getTags() != null ? normalizeTags(request.getTags()) : oldTags;
        post.setTags(serializeTags(newTags));
        if (request.getVideos() != null) post.setVideos(serializeVideos(request.getVideos()));
        postMapper.updateById(post);
        if (!oldTags.equals(newTags)) {
            applyTagCounts(oldTags, -1);
            applyTagCounts(newTags, 1);
        }
        return post;
    }
    @Transactional
    public void deletePost(Long id, String username) {
        Post post = getPost(id);
        User user = requireUser(username);
        if (!isOwnerOrAdmin(post, user)) {
            throw new RuntimeException("无权删除此帖子");
        }
        // 级联清理，避免留下孤儿评论/点赞/收藏/通知
        commentMapper.delete(Wrappers.<Comment>lambdaQuery().eq(Comment::getPostId, id));
        likeMapper.delete(Wrappers.<Like>lambdaQuery().eq(Like::getPostId, id));
        collectMapper.delete(Wrappers.<Collect>lambdaQuery().eq(Collect::getPostId, id));
        notificationMapper.delete(Wrappers.<com.forum.entity.Notification>lambdaQuery()
                .eq(com.forum.entity.Notification::getRelatedId, id));
        postMapper.deleteById(id);
        applyTagCounts(parseTags(post.getTags()), -1);
        deleteImagesIfUnused(post.getImages());
    }
    public PageResult<Post> getUserPosts(Long userId, int page, int size) {
        int safePage = Paging.page(page);
        int safeSize = Paging.size(size);
        Page<Post> postPage = new Page<>(safePage, safeSize);
        postMapper.selectPage(postPage, Wrappers.<Post>lambdaQuery()
                .eq(Post::getUserId, userId)
                .orderByDesc(Post::getCreatedAt));
        return PageResult.of(postPage.getRecords(), postPage.getTotal(), safePage, safeSize);
    }
    /** 搜索走 SQL 分页，不再把整表读进 JVM。 */
    public PageResult<Post> searchPosts(String keyword, int page, int size) {
        int safePage = Paging.page(page);
        int safeSize = Paging.size(size);
        String kw = keyword == null ? "" : keyword.trim();
        if (kw.isEmpty()) {
            return PageResult.of(List.of(), 0, safePage, safeSize);
        }
        String escaped = Paging.escapeLike(kw);
        Page<Post> postPage = new Page<>(safePage, safeSize);
        postMapper.selectPage(postPage, Wrappers.<Post>lambdaQuery()
                .and(w -> w.like(Post::getTitle, escaped)
                        .or().like(Post::getContent, escaped)
                        .or().like(Post::getTags, escaped))
                .orderByDesc(Post::getCreatedAt));
        return PageResult.of(postPage.getRecords(), postPage.getTotal(), safePage, safeSize);
    }
    /** 管理端删除标签时，同步把该标签从所有帖子的 tags 中摘掉。 */
    @Transactional
    public int removeTagFromAllPosts(String tagName) {
        if (tagName == null || tagName.isBlank()) return 0;
        List<Post> posts = postMapper.selectList(Wrappers.<Post>lambdaQuery()
                .like(Post::getTags, "\"" + Paging.escapeLike(tagName) + "\""));
        int affected = 0;
        for (Post post : posts) {
            List<String> tags = parseTags(post.getTags());
            if (tags.removeIf(t -> t.equals(tagName))) {
                post.setTags(serializeTags(tags));
                postMapper.updateById(post);
                affected++;
            }
        }
        return affected;
    }
    /* ── 标签工具 ── */
    public String serializeTags(List<String> tags) {
        List<String> clean = normalizeTags(tags);
        if (clean.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(clean);
        } catch (Exception e) {
            return null;
        }
    }
    public List<String> parseTags(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    /** 视频链接：白名单校验后抓取标题/封面，存成对象数组，前端直接渲染。 */
    private String serializeVideos(List<String> videos) {
        List<com.forum.dto.VideoMeta> metas = videoMetaService.enrich(videos);
        if (metas.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(metas);
        } catch (Exception e) {
            return null;
        }
    }
    private List<String> normalizeTags(List<String> tags) {
        if (tags == null) return new ArrayList<>();
        return tags.stream()
                .filter(java.util.Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .limit(3)
                .toList();
    }
    private void applyTagCounts(List<String> tags, int delta) {
        if (tags == null) return;
        normalizeTags(tags).forEach(name -> {
            int updated = tagMapper.addPostCount(name, delta);
            if (updated == 0 && delta > 0) {
                Tag tag = new Tag();
                tag.setName(name);
                tag.setPostCount(delta);
                try {
                    tagMapper.insert(tag);
                } catch (DuplicateKeyException e) {
                    // 并发创建同名标签：唯一键兜底后重新累加
                    tagMapper.addPostCount(name, delta);
                }
            }
        });
    }
    private String normalizeImages(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            List<String> list = objectMapper.readValue(raw, new TypeReference<List<String>>() {});
            List<String> clean = list.stream()
                    .filter(java.util.Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> s.startsWith("/picture/") || s.startsWith("https://") || s.startsWith("http://"))
                    .limit(9)
                    .toList();
            return clean.isEmpty() ? null : objectMapper.writeValueAsString(clean);
        } catch (Exception e) {
            return null;
        }
    }
    private void deleteImagesIfUnused(String imagesJson) {
        for (String url : parseTags(imagesJson)) {
            if (!url.startsWith("/picture/")) continue;
            Long stillUsed = postMapper.selectCount(Wrappers.<Post>lambdaQuery()
                    .and(w -> w.like(Post::getImages, url).or().like(Post::getContent, url)));
            if (stillUsed != null && stillUsed > 0) continue;
            String fileName = url.substring("/picture/".length());
            if (fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) continue;
            Path target = uploadRoot.resolve(fileName).normalize();
            if (!target.startsWith(uploadRoot)) continue;
            try {
                Files.deleteIfExists(target);
            } catch (IOException e) {
                log.warn("failed to delete unused image {}", target, e);
            }
        }
    }
    private boolean isOwnerOrAdmin(Post post, User user) {
        boolean owner = post.getUserId() != null && post.getUserId().equals(user.getId());
        return owner || "ADMIN".equals(user.getRole());
    }
    private String displayName(User user) {
        String nickname = user.getNickname();
        return nickname == null || nickname.isBlank() ? user.getUsername() : nickname;
    }
    private User requireUser(String username) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, username));
        if (user == null) throw new RuntimeException("用户不存在");
        return user;
    }
}
