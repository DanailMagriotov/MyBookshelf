package app.security;

import app.exception.NotAuthenticatedException;
import app.model.dto.user.UserSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationGuard {

    public void requireAuthenticated(UserSession userSession) {
        if (userSession == null) {
            throw new NotAuthenticatedException();
        }
    }

    public void requireAuthenticated(UserDetails userDetails) {
        if (userDetails == null) {
            throw new NotAuthenticatedException();
        }
    }

    public void requireAuthenticated(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new NotAuthenticatedException();
        }
    }
}
