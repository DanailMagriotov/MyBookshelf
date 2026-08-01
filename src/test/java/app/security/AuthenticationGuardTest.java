package app.security;

import app.exception.AccessDeniedException;
import app.exception.NotAuthenticatedException;
import app.model.dto.user.UserSession;
import app.model.entity.user.Region;
import app.model.entity.user.UserRole;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticationGuardTest {

    private final AuthenticationGuard guard = new AuthenticationGuard();

    @Test
    void requireAuthenticated_throwsWhenSessionMissing() {
        assertThatThrownBy(() -> guard.requireAuthenticated(null))
                .isInstanceOf(NotAuthenticatedException.class);
    }

    @Test
    void requireAuthenticated_passesForValidSession() {
        UserSession session = UserSession.builder()
                .username("alice")
                .role(UserRole.USER)
                .region(Region.SOFIA)
                .build();

        assertThatCode(() -> guard.requireAuthenticated(session)).doesNotThrowAnyException();
    }

    @Test
    void requireAdmin_throwsForRegularUser() {
        UserSession session = UserSession.builder()
                .username("alice")
                .role(UserRole.USER)
                .region(Region.SOFIA)
                .build();

        assertThatThrownBy(() -> guard.requireAdmin(session))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void requireAdmin_passesForAdmin() {
        UserSession session = UserSession.builder()
                .username("admin")
                .role(UserRole.ADMIN)
                .region(Region.SOFIA)
                .build();

        assertThatCode(() -> guard.requireAdmin(session)).doesNotThrowAnyException();
    }

    @Test
    void requireAdmin_passesForMasterAdmin() {
        UserSession session = UserSession.builder()
                .username("owner")
                .role(UserRole.MASTER_ADMIN)
                .region(Region.SOFIA)
                .build();

        assertThatCode(() -> guard.requireAdmin(session)).doesNotThrowAnyException();
    }
}
