package com.forum.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.List;
/**
 * CORS 白名单。
 * 支持通配写法（如 http://localhost:*），方便 Vite 换端口时仍能通过代理访问；
 * 生产环境请用 APP_CORS_ORIGINS 显式配置为真实域名。
 */
@Configuration
public class CorsConfig {
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") String allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        // 通配项走 allowedOriginPatterns（allowCredentials=true 时不能用 "*" 字面量）
        List<String> exact = origins.stream().filter(s -> !s.contains("*")).toList();
        List<String> patterns = origins.stream().filter(s -> s.contains("*")).toList();
        if (!exact.isEmpty()) config.setAllowedOrigins(exact);
        if (!patterns.isEmpty()) config.setAllowedOriginPatterns(patterns);
        config.setAllowCredentials(true);
        config.setAllowedHeaders(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
