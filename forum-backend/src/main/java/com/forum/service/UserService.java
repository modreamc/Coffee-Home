package com.forum.service;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.forum.config.JwtUtil;
import com.forum.dto.LoginRequest;
import com.forum.dto.RegisterRequest;
import com.forum.dto.UserInfo;
import com.forum.entity.Collect;
import com.forum.entity.Comment;
import com.forum.entity.Like;
import com.forum.entity.Post;
import com.forum.entity.SignIn;
import com.forum.entity.User;
import com.forum.exception.TooManyRequestsException;
import com.forum.mapper.CollectMapper;
import com.forum.mapper.CommentMapper;
import com.forum.mapper.LikeMapper;
import com.forum.mapper.PostMapper;
import com.forum.mapper.SignInMapper;
import com.forum.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.security.SecureRandom;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserMapper userMapper;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final LikeMapper likeMapper;
    private final CollectMapper collectMapper;
    private final SignInMapper signInMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final LoginAttemptService loginAttemptService;
    private final PostService postService;
    private final CommentService commentService;
    private final NotificationService notificationService;
    /** 用于"用户不存在"时也执行一次哈希比对，抹平响应时间差异。 */
    private final String dummyHash;

    @Value("${app.admin.username}")
    private String adminUsername;
    @Value("${app.admin.password}")
    private String adminPassword;
    @Value("${app.admin.auto-init:true}")
    private boolean adminAutoInit;

    public UserService(UserMapper userMapper, PostMapper postMapper, CommentMapper commentMapper,
                       LikeMapper likeMapper, CollectMapper collectMapper, SignInMapper signInMapper,
                       PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                       LoginAttemptService loginAttemptService, PostService postService,
                       CommentService commentService, NotificationService notificationService) {
        this.userMapper = userMapper;
        this.postMapper = postMapper;
        this.commentMapper = commentMapper;
        this.likeMapper = likeMapper;
        this.collectMapper = collectMapper;
        this.signInMapper = signInMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.loginAttemptService = loginAttemptService;
        this.postService = postService;
        this.commentService = commentService;
        this.notificationService = notificationService;
        this.dummyHash = passwordEncoder.encode("dummy-password-for-timing-equalization");
    }

    public void initAdmin() {
        if (!adminAutoInit) return;
        if (userMapper.selectCount(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, adminUsername)) == 0) {
            String password = adminPassword;
            boolean generated = false;
            if (password == null || password.isBlank()) {
                // 公开仓库不内置默认密码：首次启动生成随机密码并打印一次
                password = generatePassword(12);
                generated = true;
            }
            User admin = new User();
            admin.setUsername(adminUsername);
            admin.setPassword(passwordEncoder.encode(password));
            admin.setNickname("管理员");
            admin.setRole("ADMIN");
            userMapper.insert(admin);
            log.info("initial admin account '{}' created", adminUsername);
            if (generated) {
                log.warn("ADMIN_PASSWORD 未配置，已生成随机管理员密码：{}（请登录后立即修改，或设置 ADMIN_PASSWORD 环境变量后重建）", password);
            }
        }
    }

    private String generatePassword(int length) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public Map<String, Object> login(LoginRequest request, String clientKey) {
        String username = request.getUsername() == null ? "" : request.getUsername().trim();
        String key = clientKey + "|" + username;
        if (loginAttemptService.isBlocked(key)) {
            long minutes = Math.max(1, loginAttemptService.remainingLockSeconds(key) / 60);
            throw new TooManyRequestsException("登录失败次数过多，请 " + minutes + " 分钟后再试");
        }
        User user = findByUsernameOrNull(username);
        // 用户不存在时也做一次哈希比对，避免通过响应时间/文案枚举账号
        String encoded = user != null && user.getPassword() != null ? user.getPassword() : dummyHash;
        boolean matches = passwordEncoder.matches(request.getPassword(), encoded);
        if (user == null || !matches) {
            loginAttemptService.onFailure(key);
            throw new RuntimeException("用户名或密码错误");
        }
        loginAttemptService.onSuccess(key);
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
        return Map.of("token", token, "user", toUserInfo(user));
    }

    @Transactional
    public Map<String, Object> register(RegisterRequest request) {
        String username = request.getUsername().trim();
        if (findByUsernameOrNull(username) != null) {
            throw new RuntimeException("用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        String nickname = request.getNickname() == null ? null : request.getNickname().trim();
        user.setNickname(nickname == null || nickname.isEmpty() ? username : nickname);
        user.setEmail(request.getEmail() == null || request.getEmail().isBlank() ? null : request.getEmail().trim());
        user.setRole("USER");
        user.setPoints(0);
        userMapper.insert(user);
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
        return Map.of("token", token, "user", toUserInfo(user));
    }

    public UserInfo getCurrentUser(String username) {
        return toUserInfo(findByUsername(username));
    }

    /**
     * 管理员删除用户：级联清理其帖子、评论（含楼中楼）、点赞、收藏、签到、通知。
     * 不能删除自己，也不能删掉最后一个管理员。
     */
    @Transactional
    public void deleteUser(Long userId, String operatorUsername) {
        User target = userMapper.selectById(userId);
        if (target == null) throw new RuntimeException("用户不存在");
        User operator = findByUsername(operatorUsername);
        if (target.getId().equals(operator.getId())) {
            throw new RuntimeException("不能删除自己的账号");
        }
        if ("ADMIN".equals(target.getRole())) {
            long adminCount = userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getRole, "ADMIN"));
            if (adminCount <= 1) throw new RuntimeException("系统至少需要保留一个管理员");
        }
        List<Post> posts = postMapper.selectList(Wrappers.<Post>lambdaQuery().eq(Post::getUserId, userId));
        for (Post post : posts) {
            postService.deletePost(post.getId(), operatorUsername);
        }
        commentService.deleteCommentsByUser(userId);
        List<Like> likes = likeMapper.selectList(Wrappers.<Like>lambdaQuery().eq(Like::getUserId, userId));
        for (Like like : likes) {
            likeMapper.deleteById(like.getId());
            postMapper.addLikeCount(like.getPostId(), -1);
        }
        List<Collect> collects = collectMapper.selectList(Wrappers.<Collect>lambdaQuery().eq(Collect::getUserId, userId));
        for (Collect collect : collects) {
            collectMapper.deleteById(collect.getId());
            postMapper.addCollectCount(collect.getPostId(), -1);
        }
        signInMapper.delete(Wrappers.<SignIn>lambdaQuery().eq(SignIn::getUserId, userId));
        notificationService.deleteByUser(userId);
        userMapper.deleteById(userId);
        log.info("admin '{}' deleted user '{}' (#{})", operatorUsername, target.getUsername(), userId);
    }

    public UserInfo toUserInfo(User user) {
        UserInfo info = new UserInfo();
        info.setId(user.getId());
        info.setUsername(user.getUsername());
        info.setNickname(user.getNickname());
        info.setAvatar(user.getAvatar());
        info.setEmail(user.getEmail());
        info.setRole(user.getRole());
        info.setPoints(user.getPoints() != null ? user.getPoints() : 0);
        info.setLevel(levelName(user.getPoints() != null ? user.getPoints() : 0));
        return info;
    }

    /** 按积分计算港湾等级称号 */
    public static String levelName(int points) {
        if (points >= 400) return "传奇水手 🌊";
        if (points >= 150) return "港湾老船长 ⚓";
        if (points >= 50) return "港湾常客 ☕";
        return "港湾新人 🌱";
    }

    public User findByUsername(String username) {
        User user = findByUsernameOrNull(username);
        if (user == null) throw new RuntimeException("用户不存在");
        return user;
    }

    public User findByUsernameOrNull(String username) {
        if (username == null || username.isBlank()) return null;
        return userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, username.trim()));
    }
}
