package com.forum.service;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.forum.entity.SignIn;
import com.forum.entity.User;
import com.forum.mapper.SignInMapper;
import com.forum.mapper.UserMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
@Service
public class SignInService {
    private static final int REWARD_POINTS = 10;
    /** 与数据库 serverTimezone 保持一致，避免服务器时区变化导致跨天重复签到。 */
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private final SignInMapper signInMapper;
    private final UserMapper userMapper;
    public SignInService(SignInMapper signInMapper, UserMapper userMapper) {
        this.signInMapper = signInMapper;
        this.userMapper = userMapper;
    }
    @Transactional
    public Map<String, Object> signIn(String username) {
        User user = requireUser(username);
        LocalDate today = LocalDate.now(ZONE);
        if (alreadySigned(user.getId(), today)) {
            return Map.of("signed", true, "already", true, "points", pointsOf(user));
        }
        SignIn signIn = new SignIn();
        signIn.setUserId(user.getId());
        signIn.setSignDate(today);
        try {
            signInMapper.insert(signIn);
        } catch (DuplicateKeyException e) {
            // 并发双击时唯一键兜底，不能重复加积分
            return Map.of("signed", true, "already", true, "points", pointsOf(user));
        }
        userMapper.addPoints(user.getId(), REWARD_POINTS);
        return Map.of("signed", true, "already", false,
                "points", pointsOf(user) + REWARD_POINTS, "reward", REWARD_POINTS);
    }
    public boolean isSignedToday(String username) {
        User user = requireUser(username);
        return alreadySigned(user.getId(), LocalDate.now(ZONE));
    }
    private boolean alreadySigned(Long userId, LocalDate day) {
        return signInMapper.selectCount(Wrappers.<SignIn>lambdaQuery()
                .eq(SignIn::getUserId, userId)
                .eq(SignIn::getSignDate, day)) > 0;
    }
    private int pointsOf(User user) {
        return user.getPoints() == null ? 0 : user.getPoints();
    }
    private User requireUser(String username) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, username));
        if (user == null) throw new RuntimeException("用户不存在");
        return user;
    }
}
