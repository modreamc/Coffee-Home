package com.forum.service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
/**
 * 登录失败限流（按 "IP|用户名" 维度）。
 * 内存实现，单实例够用；集群部署时替换为 Redis 即可。
 */
@Service
public class LoginAttemptService {
    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);
    private static final int CLEANUP_THRESHOLD = 10_000;

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    @Value("${app.security.login-max-attempts:5}")
    private int maxAttempts;
    @Value("${app.security.login-window-seconds:900}")
    private long windowSeconds;
    @Value("${app.security.login-lock-seconds:900}")
    private long lockSeconds;

    private static final class Attempt {
        int failures;
        long windowStartMillis;
        long lockedUntilMillis;
    }

    public boolean isBlocked(String key) {
        Attempt a = attempts.get(key);
        return a != null && a.lockedUntilMillis > System.currentTimeMillis();
    }

    public long remainingLockSeconds(String key) {
        Attempt a = attempts.get(key);
        if (a == null) return 0;
        long left = a.lockedUntilMillis - System.currentTimeMillis();
        return left <= 0 ? 0 : (left + 999) / 1000;
    }

    public void onFailure(String key) {
        long now = System.currentTimeMillis();
        attempts.compute(key, (k, a) -> {
            if (a == null) a = new Attempt();
            if (a.windowStartMillis == 0 || now - a.windowStartMillis > windowSeconds * 1000) {
                a.failures = 0;
                a.windowStartMillis = now;
            }
            a.failures++;
            if (a.failures >= maxAttempts) {
                a.lockedUntilMillis = now + lockSeconds * 1000;
                a.failures = 0;
                a.windowStartMillis = 0;
                log.warn("login blocked for key={} for {}s", key, lockSeconds);
            }
            return a;
        });
        cleanupIfNeeded();
    }

    public void onSuccess(String key) {
        attempts.remove(key);
    }

    private void cleanupIfNeeded() {
        if (attempts.size() <= CLEANUP_THRESHOLD) return;
        long now = System.currentTimeMillis();
        attempts.entrySet().removeIf(e -> {
            Attempt a = e.getValue();
            return a.lockedUntilMillis < now && now - a.windowStartMillis > windowSeconds * 1000;
        });
    }
}
