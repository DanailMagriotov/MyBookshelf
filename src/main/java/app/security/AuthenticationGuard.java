package app.security;

import app.exception.NotAuthenticatedException;
import app.exception.AccessDeniedException;
import app.model.dto.user.UserSession;
import app.model.entity.user.UserRole;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationGuard {

    public void requireAuthenticated(UserSession userSession) {
        if (userSession == null) {
            throw new NotAuthenticatedException();
        }
    }

    public void requireAdmin(UserSession userSession) {
        requireAuthenticated(userSession);
        if (userSession.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException();
        }
    }
}
