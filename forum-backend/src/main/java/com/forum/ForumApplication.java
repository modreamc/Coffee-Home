package com.forum;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
/**
 * 排除 UserDetailsServiceAutoConfiguration：
 * 我们使用无状态的 JWT 过滤器链，并不需要 Spring Boot 自动生成的 in-memory 用户，
 * 否则启动日志会打印一个"generated security password"，容易误导运维以为存在默认账号。
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@MapperScan("com.forum.mapper")
public class ForumApplication {
    public static void main(String[] args) {
        SpringApplication.run(ForumApplication.class, args);
    }
}
