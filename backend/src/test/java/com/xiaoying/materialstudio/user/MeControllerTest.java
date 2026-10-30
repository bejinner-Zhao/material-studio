package com.xiaoying.materialstudio.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MeControllerTest {

    @Mock
    private UserService userService;

    private MeController controller;

    @BeforeEach
    void setUp() {
        controller = new MeController(userService);
        when(userService.resolveOrCreate(anyString())).thenAnswer(invocation -> {
            String publicId = invocation.getArgument(0);
            AppUser user = new AppUser();
            user.setId(7L);
            user.setPublicId(publicId);
            user.setNickname("访客-" + publicId.substring(0, 4));
            return user;
        });
    }

    @Test
    @DisplayName("切换身份：下发新的 HttpOnly Cookie 并返回新身份")
    void resetIssuesNewIdentityCookie() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        MeController.MeResponse me = controller.reset(response);

        assertThat(me.id()).isEqualTo(7L);

        String setCookie = response.getHeader("Set-Cookie");
        assertThat(setCookie)
                .as("必须覆盖同一个 Cookie 名，且保持 HttpOnly")
                .contains("ms_uid=")
                .contains("HttpOnly")
                .contains("Path=/");
    }

    @Test
    @DisplayName("切换两次得到两个不同的身份")
    void resetGeneratesDistinctIdentityEveryTime() {
        controller.reset(new MockHttpServletResponse());
        controller.reset(new MockHttpServletResponse());

        ArgumentCaptor<String> publicIds = ArgumentCaptor.forClass(String.class);
        verify(userService, times(2)).resolveOrCreate(publicIds.capture());

        List<String> values = publicIds.getAllValues();
        assertThat(values.get(0)).isNotEqualTo(values.get(1));
        assertThat(values).allSatisfy(id -> assertThat(id).hasSize(36));
    }
}
