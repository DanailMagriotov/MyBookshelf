package app.security;

import app.exception.NotAuthenticatedException;
import app.model.dto.user.UserSession;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationGuard {

    public void requireAuthenticated(UserSession userSession) {
        if (userSession == null) {
            throw new NotAuthenticatedException();
        }
    }
}
