package app.security;

import app.service.user.UserSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

@NullMarked
@Component
public class UserSessionLogoutHandler implements LogoutHandler {

    private final UserSessionService userSessionService;

    public UserSessionLogoutHandler(UserSessionService userSessionService) {
        this.userSessionService = userSessionService;
    }

    @Override
    public void logout(HttpServletRequest request,
                       HttpServletResponse response,
                       @Nullable Authentication authentication) {
        userSessionService.clear(request.getSession(false));
    }
}
