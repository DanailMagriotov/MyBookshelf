package app.security;

import app.exception.NotAuthenticatedException;
import app.exception.AccessDeniedException;
import app.model.dto.user.UserSession;
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
        if (!userSession.getRole().isAdmin()) {
            throw new AccessDeniedException();
        }
    }
}
