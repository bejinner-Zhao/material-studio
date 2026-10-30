package com.xiaoying.materialstudio.user;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

/**
 * 轻量身份：Cookie 中保存 UUID 作为 public_id，首次访问自动建用户。
 * 解析结果放在 request attribute 中，接口通过 @RequestAttribute("currentUserId") 取用。
 */
@Component
public class UserIdentityFilter extends OncePerRequestFilter {

    public static final String CURRENT_USER_ID = "currentUserId";
    public static final String COOKIE_NAME = "ms_uid";

    private static final String RESET_PATH = "/api/me/reset";

    private static final Duration COOKIE_MAX_AGE = Duration.ofDays(365);

    private final UserService userService;

    public UserIdentityFilter(UserService userService) {
        this.userService = userService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 切换身份的接口自己签发新 Cookie，不参与本请求的身份解析，避免下发两次 ms_uid
        if (RESET_PATH.equals(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String publicId = readPublicId(request);
        boolean newlyIssued = false;
        if (publicId == null) {
            publicId = UUID.randomUUID().toString();
            newlyIssued = true;
        }

        AppUser user = userService.resolveOrCreate(publicId);
        request.setAttribute(CURRENT_USER_ID, user.getId());

        if (newlyIssued) {
            response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(publicId).toString());
        }

        filterChain.doFilter(request, response);
    }

    public static ResponseCookie buildCookie(String publicId) {
        return ResponseCookie.from(COOKIE_NAME, publicId)
                .httpOnly(true)
                .path("/")
                .maxAge(COOKIE_MAX_AGE)
                .sameSite("Lax")
                .build();
    }

    /** 只接受合法 UUID，避免被构造的超长值写进 CHAR(36) 列 */
    private static String readPublicId(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (!COOKIE_NAME.equals(cookie.getName())) {
                continue;
            }
            String value = cookie.getValue();
            if (value == null || value.length() != 36) {
                return null;
            }
            try {
                UUID.fromString(value);
                return value;
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }
        return null;
    }
}
