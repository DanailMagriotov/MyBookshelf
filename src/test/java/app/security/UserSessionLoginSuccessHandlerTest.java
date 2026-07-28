package app.security;

import app.model.dto.user.UserSession;
import app.model.entity.user.Region;
import app.model.entity.user.User;
import app.model.entity.user.UserRole;
import app.repository.user.UserRepository;
import app.service.user.UserSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserSessionLoginSuccessHandlerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSessionService userSessionService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HttpSession session;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserSessionLoginSuccessHandler handler;

    @Test
    void onAuthenticationSuccess_savesSessionAndRedirects() throws Exception {
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("alice")
                .email("alice@example.com")
                .role(UserRole.USER)
                .region(Region.SOFIA)
                .build();

        when(authentication.getName()).thenReturn("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(request.getSession()).thenReturn(session);
        when(request.getContextPath()).thenReturn("");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(userSessionService).save(org.mockito.ArgumentMatchers.eq(session), org.mockito.ArgumentMatchers.any(UserSession.class));
        verify(response).sendRedirect("/home");
    }
}
