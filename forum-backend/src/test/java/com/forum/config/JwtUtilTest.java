package com.forum.config;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
class JwtUtilTest {
    private static final String SECRET = "unit-test-secret-key-must-be-at-least-32-bytes-long";

    @Test
    void generatesAndParsesToken() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 60_000);
        String token = jwtUtil.generateToken("alice", "USER");
        assertThat(jwtUtil.validateToken(token)).isTrue();
        assertThat(jwtUtil.getUsernameFromToken(token)).isEqualTo("alice");
        assertThat(jwtUtil.getRoleFromToken(token)).isEqualTo("USER");
    }

    @Test
    void rejectsExpiredToken() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, -1_000);
        String token = jwtUtil.generateToken("bob", "USER");
        assertThat(jwtUtil.validateToken(token)).isFalse();
    }

    @Test
    void rejectsTamperedPayload() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 60_000);
        String token = jwtUtil.generateToken("carol", "USER");
        String[] parts = token.split("\\.");
        // 改写 payload 里的一个字符，签名必须校验失败
        char[] payload = parts[1].toCharArray();
        payload[0] = payload[0] == 'e' ? 'f' : 'e';
        String tampered = parts[0] + "." + new String(payload) + "." + parts[2];
        assertThat(jwtUtil.validateToken(tampered)).isFalse();
    }

    @Test
    void rejectsTokenSignedWithAnotherKey() {
        String other = "another-secret-key-that-is-long-enough-for-hs256!!";
        String token = new JwtUtil(other, 60_000).generateToken("dave", "ADMIN");
        assertThat(new JwtUtil(SECRET, 60_000).validateToken(token)).isFalse();
    }
}
