package com.xiaoying.materialstudio.user;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final UserService userService;

    public MeController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public MeResponse me(@RequestAttribute(UserIdentityFilter.CURRENT_USER_ID) Long userId) {
        AppUser user = userService.getById(userId);
        return new MeResponse(user.getId(), user.getNickname());
    }

    /**
     * 切换身份：签发一个新的匿名 publicId 并覆盖 Cookie。
     * 原身份的数据仍然保留在库里，只是当前浏览器不再访问它。
     */
    @PostMapping("/reset")
    public MeResponse reset(HttpServletResponse response) {
        String publicId = UUID.randomUUID().toString();
        AppUser user = userService.resolveOrCreate(publicId);
        response.addHeader(HttpHeaders.SET_COOKIE, UserIdentityFilter.buildCookie(publicId).toString());
        return new MeResponse(user.getId(), user.getNickname());
    }

    public record MeResponse(Long id, String nickname) {
    }
}
