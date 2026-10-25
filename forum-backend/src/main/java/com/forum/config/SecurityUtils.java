package com.forum.config;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
/** 统一处理"匿名用户"，避免到处写 auth != null 却拿到 anonymousUser。 */
public final class SecurityUtils {
    private SecurityUtils() {}
    public static boolean isLoggedIn(Authentication auth) {
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }
    public static String usernameOrNull(Authentication auth) {
        return isLoggedIn(auth) ? auth.getName() : null;
    }
    public static boolean isAdmin(Authentication auth) {
        return isLoggedIn(auth) && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
    public static String currentUsername() {
        return usernameOrNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
