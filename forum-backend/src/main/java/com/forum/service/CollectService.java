package com.forum.service;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.forum.dto.PageResult;
import com.forum.entity.Collect;
import com.forum.entity.Post;
import com.forum.entity.User;
import com.forum.mapper.CollectMapper;
import com.forum.mapper.PostMapper;
import com.forum.mapper.UserMapper;
import com.forum.util.Paging;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
@Service
public class CollectService {
    private final CollectMapper collectMapper;
    private final PostMapper postMapper;
    private final UserMapper userMapper;
    public CollectService(CollectMapper collectMapper, PostMapper postMapper, UserMapper userMapper) {
        this.collectMapper = collectMapper;
        this.postMapper = postMapper;
        this.userMapper = userMapper;
    }
    @Transactional
    public Map<String, Object> toggleCollect(Long postId, String username) {
        User user = requireUser(username);
        Post post = postMapper.selectById(postId);
        if (post == null) throw new RuntimeException("帖子不存在");
        int removed = collectMapper.delete(Wrappers.<Collect>lambdaQuery()
                .eq(Collect::getUserId, user.getId())
                .eq(Collect::getPostId, postId));
        if (removed > 0) {
            postMapper.addCollectCount(postId, -1);
            return result(false, postId);
        }
        Collect collect = new Collect();
        collect.setUserId(user.getId());
        collect.setPostId(postId);
        try {
            collectMapper.insert(collect);
        } catch (DuplicateKeyException e) {
            return result(true, postId);
        }
        postMapper.addCollectCount(postId, 1);
        return result(true, postId);
    }
    public boolean isCollected(Long postId, String username) {
        if (username == null) return false;
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, username));
        if (user == null) return false;
        return collectMapper.selectCount(Wrappers.<Collect>lambdaQuery()
                .eq(Collect::getUserId, user.getId())
                .eq(Collect::getPostId, postId)) > 0;
    }
    public PageResult<Post> getUserCollects(String username, int page, int size) {
        User user = requireUser(username);
        Page<Collect> collectPage = new Page<>(Paging.page(page), Paging.size(size));
        collectMapper.selectPage(collectPage, Wrappers.<Collect>lambdaQuery()
                .eq(Collect::getUserId, user.getId())
                .orderByDesc(Collect::getCreatedAt));
        List<Long> postIds = collectPage.getRecords().stream().map(Collect::getPostId).toList();
        if (postIds.isEmpty()) {
            return PageResult.of(List.of(), collectPage.getTotal(), page, size);
        }
        // 批量查询，避免逐条 selectById 的 N+1
        Map<Long, Post> byId = postMapper.selectBatchIds(postIds).stream()
                .collect(Collectors.toMap(Post::getId, Function.identity()));
        List<Post> posts = new ArrayList<>();
        for (Long id : postIds) {
            Post post = byId.get(id);
            if (post != null) posts.add(post);
        }
        return PageResult.of(posts, collectPage.getTotal(), page, size);
    }
    private Map<String, Object> result(boolean collected, Long postId) {
        Post fresh = postMapper.selectById(postId);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("collected", collected);
        body.put("collectCount", fresh == null || fresh.getCollectCount() == null ? 0 : fresh.getCollectCount());
        return body;
    }
    private User requireUser(String username) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, username));
        if (user == null) throw new RuntimeException("用户不存在");
        return user;
    }
}
