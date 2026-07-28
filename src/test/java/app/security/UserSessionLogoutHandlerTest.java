package app.security;

import app.service.user.UserSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserSessionLogoutHandlerTest {

    @Mock
    private UserSessionService userSessionService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HttpSession session;

    @InjectMocks
    private UserSessionLogoutHandler handler;

    @Test
    void logout_clearsUserSession() {
        when(request.getSession(false)).thenReturn(session);

        handler.logout(request, response, null);

        verify(userSessionService).clear(session);
    }
}
