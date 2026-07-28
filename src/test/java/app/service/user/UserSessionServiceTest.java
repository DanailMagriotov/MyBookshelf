package app.service.user;

import app.model.dto.user.UserSession;
import app.model.entity.user.Region;
import app.model.entity.user.UserRole;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserSessionServiceTest {

    @Mock
    private HttpSession session;

    @InjectMocks
    private UserSessionService userSessionService;

    @Test
    void save_storesSessionAttribute() {
        UserSession userSession = UserSession.builder()
                .id(UUID.randomUUID())
                .username("alice")
                .role(UserRole.USER)
                .region(Region.SOFIA)
                .build();

        userSessionService.save(session, userSession);

        verify(session).setAttribute(UserSessionService.SESSION_ATTRIBUTE, userSession);
    }

    @Test
    void get_returnsEmptyWhenSessionMissing() {
        assertThat(userSessionService.get(null)).isEmpty();
    }

    @Test
    void get_returnsUserSessionWhenPresent() {
        UserSession userSession = UserSession.builder()
                .username("alice")
                .role(UserRole.USER)
                .region(Region.SOFIA)
                .build();
        when(session.getAttribute(UserSessionService.SESSION_ATTRIBUTE)).thenReturn(userSession);

        Optional<UserSession> result = userSessionService.get(session);

        assertThat(result).contains(userSession);
    }

    @Test
    void get_returnsEmptyForUnexpectedAttributeType() {
        when(session.getAttribute(UserSessionService.SESSION_ATTRIBUTE)).thenReturn("invalid");

        assertThat(userSessionService.get(session)).isEmpty();
    }

    @Test
    void clear_removesAttributeWhenSessionExists() {
        userSessionService.clear(session);

        verify(session).removeAttribute(UserSessionService.SESSION_ATTRIBUTE);
    }

    @Test
    void clear_ignoresNullSession() {
        userSessionService.clear(null);
    }
}
