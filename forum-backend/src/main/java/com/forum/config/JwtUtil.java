package com.forum.config;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
@Component
public class JwtUtil {
    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);
    private final SecretKey key;
    private final long expiration;
    public JwtUtil(@Value("${jwt.secret:}") String secret,
                   @Value("${jwt.expiration}") long expiration) {
        if (secret == null || secret.isBlank()) {
            // 公开仓库不内置密钥：未配置时生成临时密钥，只适合本地开发
            byte[] random = new byte[48];
            new SecureRandom().nextBytes(random);
            secret = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
            log.warn("JWT_SECRET 未配置，已生成临时密钥（重启后旧 Token 全部失效）；生产环境请设置至少 32 字节的随机 JWT_SECRET");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }
    public String generateToken(String username, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiration);
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    public boolean validateToken(String token) {
        try { parseToken(token); return true; }
        catch (Exception e) { return false; }
    }
    public String getUsernameFromToken(String token) {
        return parseToken(token).getSubject();
    }
    public String getRoleFromToken(String token) {
        return parseToken(token).get("role", String.class);
    }
}
