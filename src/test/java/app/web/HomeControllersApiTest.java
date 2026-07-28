package app.web;

import app.exception.MessageServiceUnavailableException;
import app.security.AuthenticationGuard;
import app.service.book.BookService;
import app.service.message.MessageAppService;
import app.service.user.UserSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import static app.testsupport.WebTestSupport.adminSession;
import static app.testsupport.WebTestSupport.sessionWith;
import static app.testsupport.WebTestSupport.standaloneWithPageable;
import static app.testsupport.WebTestSupport.userSession;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class HomeControllersApiTest {

    @Mock
    private UserSessionService userSessionService;

    @Mock
    private BookService bookService;

    @Mock
    private MessageAppService messageAppService;

    private MockMvc homeMockMvc;
    private MockMvc homeActionMockMvc;

    @BeforeEach
    void setUp() {
        homeMockMvc = standaloneWithPageable(new HomeController(userSessionService, bookService, messageAppService))
                .build();
        homeActionMockMvc = standaloneWithPageable(
                new HomeActionController(new AuthenticationGuard(), userSessionService)).build();
    }

    @Test
    void home_withoutSession_returnsHomeView() throws Exception {
        when(userSessionService.get(any())).thenReturn(java.util.Optional.empty());

        homeMockMvc.perform(get("/home"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"));
    }

    @Test
    void home_withSession_populatesModel() throws Exception {
        var session = userSession();
        when(userSessionService.get(any())).thenReturn(java.util.Optional.of(session));
        when(bookService.countVisibleBooksForUser(session.getId())).thenReturn(3L);
        when(messageAppService.getUnreadCount(session.getId())).thenReturn(2L);

        homeMockMvc.perform(get("/home").session(sessionWith(session)))
                .andExpect(status().isOk())
                .andExpect(view().name("home"));
    }

    @Test
    void home_whenMessagingUnavailable_usesZeroUnreadCount() throws Exception {
        var session = userSession();
        when(userSessionService.get(any())).thenReturn(java.util.Optional.of(session));
        when(bookService.countVisibleBooksForUser(session.getId())).thenReturn(1L);
        when(messageAppService.getUnreadCount(session.getId()))
                .thenThrow(new MessageServiceUnavailableException(new RuntimeException("unavailable")));

        homeMockMvc.perform(get("/home").session(sessionWith(session)))
                .andExpect(status().isOk())
                .andExpect(view().name("home"));
    }

    @Test
    void homeActions_redirectToProtectedPages() throws Exception {
        var session = userSession();
        when(userSessionService.get(any())).thenReturn(java.util.Optional.of(session));

        homeActionMockMvc.perform(get("/home/actions/bookshelf").session(sessionWith(session)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/my-bookshelf"));

        homeActionMockMvc.perform(get("/home/actions/add-book").session(sessionWith(session)))
                .andExpect(redirectedUrl("/add-book"));

        homeActionMockMvc.perform(get("/home/actions/send-book").session(sessionWith(session)))
                .andExpect(redirectedUrl("/send-book"));

        homeActionMockMvc.perform(get("/home/actions/messages").session(sessionWith(session)))
                .andExpect(redirectedUrl("/messages"));

        homeActionMockMvc.perform(get("/home/actions/my-profile").session(sessionWith(session)))
                .andExpect(redirectedUrl("/my-profile"));
    }

    @Test
    void homeActions_usersRedirect_requiresAdmin() throws Exception {
        var session = adminSession();
        when(userSessionService.get(any())).thenReturn(java.util.Optional.of(session));

        homeActionMockMvc.perform(get("/home/actions/users").session(sessionWith(session)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users"));
    }

    @Test
    void homeActions_withoutSession_redirectsToLogin() throws Exception {
        when(userSessionService.get(any())).thenReturn(java.util.Optional.empty());

        homeActionMockMvc.perform(get("/home/actions/bookshelf"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
}
