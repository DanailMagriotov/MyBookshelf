package app.service.user;

import app.model.dto.user.UserSession;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserSessionService {

    public static final String SESSION_ATTRIBUTE = "userSession";

    public void save(HttpSession session, UserSession userSession) {
        session.setAttribute(SESSION_ATTRIBUTE, userSession);
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
            session.removeAttribute(SESSION_ATTRIBUTE);
        }
    }
}
