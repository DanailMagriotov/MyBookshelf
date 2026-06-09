package app.security;

import app.mapper.user.UserMapper;
import app.repository.user.UserRepository;
import app.service.user.UserSessionService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class UserSessionLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final UserSessionService userSessionService;

    public UserSessionLoginSuccessHandler(UserRepository userRepository,
                                          UserSessionService userSessionService) {
        this.userRepository = userRepository;
        this.userSessionService = userSessionService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        userRepository.findByUsername(authentication.getName())
                .map(UserMapper::toUserSession)
                .ifPresent(userSession -> userSessionService.save(request.getSession(), userSession));

        response.sendRedirect(request.getContextPath() + "/home");
    }
}
