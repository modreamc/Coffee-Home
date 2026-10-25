package com.forum.config;
import com.forum.entity.User;
import com.forum.mapper.UserMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;
@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    public JwtAuthFilter(JwtUtil jwtUtil, UserMapper userMapper) {
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
    }
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String token = extractToken(request);
        if (token != null) {
            try {
                if (jwtUtil.validateToken(token)) {
                    authenticate(token);
                }
            } catch (Exception e) {
                // token 无效/过期不应打断请求，交给 Security 决定是否 401
                log.debug("jwt authentication skipped: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(String token) {
        Claims claims = jwtUtil.parseToken(token);
        String username = claims.getSubject();
        if (!StringUtils.hasText(username)) return;
        // 以数据库为准：用户被删除则 token 立即失效，角色变更无需重新登录
        User user = userMapper.selectOne(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<User>lambdaQuery()
                        .eq(User::getUsername, username));
        if (user == null) return;
        String role = user.getRole() == null ? "USER" : user.getRole();
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user.getUsername(), null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        authentication.setDetails(user.getId());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
