package app.service.user;

import app.model.dto.user.UserSession;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserSessionService {

    private static final Logger log = LoggerFactory.getLogger(UserSessionService.class);

    public static final String SESSION_ATTRIBUTE = "userSession";

    public void save(HttpSession session, UserSession userSession) {
        session.setAttribute(SESSION_ATTRIBUTE, userSession);
        log.info("User '{}' logged in", userSession.getUsername());
    }

    public Optional<UserSession> get(HttpSession session) {
        if (session == null) {
            return Optional.empty();
        }

        Object value = session.getAttribute(SESSION_ATTRIBUTE);
        if (value instanceof UserSession userSession) {
            return Optional.of(userSession);
        }

        return Optional.empty();
    }

    public void clear(HttpSession session) {
        if (session != null) {
            get(session).ifPresent(userSession ->
                    log.info("User '{}' logged out", userSession.getUsername()));
            session.removeAttribute(SESSION_ATTRIBUTE);
        }
    }
}
