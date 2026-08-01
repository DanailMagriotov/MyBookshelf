package app.web;

import app.exception.MessageServiceUnavailableException;
import app.service.book.BookService;
import app.service.message.MessageAppService;
import app.service.user.UserSessionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final UserSessionService userSessionService;
    private final BookService bookService;
    private final MessageAppService messageAppService;

    public HomeController(UserSessionService userSessionService,
                          BookService bookService,
                          MessageAppService messageAppService) {
        this.userSessionService = userSessionService;
        this.bookService = bookService;
        this.messageAppService = messageAppService;
    }

    @GetMapping("/home")
    public String home(HttpSession session, Model model) {
        userSessionService.get(session).ifPresent(userSession -> {
            model.addAttribute("userSession", userSession);
            model.addAttribute("username", userSession.getUsername());
            model.addAttribute("isAdmin", userSession.getRole().isAdmin());
            model.addAttribute("bookshelfCount", bookService.countVisibleBooksForUser(userSession.getId()));
            try {
                model.addAttribute("unreadMessageCount", messageAppService.getUnreadCount(userSession.getId()));
            } catch (MessageServiceUnavailableException ex) {
                model.addAttribute("unreadMessageCount", 0L);
            }
        });
        return "home";
    }
}
