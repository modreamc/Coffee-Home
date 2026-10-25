package com.forum.service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.assertThat;
class LoginAttemptServiceTest {
    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        service = new LoginAttemptService();
        ReflectionTestUtils.setField(service, "maxAttempts", 3);
        ReflectionTestUtils.setField(service, "windowSeconds", 900L);
        ReflectionTestUtils.setField(service, "lockSeconds", 900L);
    }

    @Test
    void blocksAfterMaxAttempts() {
        String key = "127.0.0.1|alice";
        service.onFailure(key);
        service.onFailure(key);
        assertThat(service.isBlocked(key)).isFalse();
        service.onFailure(key);
        assertThat(service.isBlocked(key)).isTrue();
        assertThat(service.remainingLockSeconds(key)).isGreaterThan(0);
    }

    @Test
    void successfulLoginClearsFailures() {
        String key = "127.0.0.1|bob";
        service.onFailure(key);
        service.onFailure(key);
        service.onSuccess(key);
        service.onFailure(key);
        service.onFailure(key);
        assertThat(service.isBlocked(key)).isFalse();
    }

    @Test
    void differentKeysAreIndependent() {
        service.onFailure("ip1|a");
        service.onFailure("ip1|a");
        service.onFailure("ip1|a");
        assertThat(service.isBlocked("ip1|a")).isTrue();
        assertThat(service.isBlocked("ip2|a")).isFalse();
    }
}
