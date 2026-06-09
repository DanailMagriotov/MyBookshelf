package app.web;

import app.model.entity.user.UserRole;
import app.service.book.BookService;
import app.service.user.UserSessionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final UserSessionService userSessionService;
    private final BookService bookService;

    public HomeController(UserSessionService userSessionService, BookService bookService) {
        this.userSessionService = userSessionService;
        this.bookService = bookService;
    }

    @GetMapping("/home")
    public String home(HttpSession session, Model model) {
        userSessionService.get(session).ifPresent(userSession -> {
            model.addAttribute("userSession", userSession);
            model.addAttribute("username", userSession.getUsername());
            model.addAttribute("isAdmin", userSession.getRole() == UserRole.ADMIN);
            model.addAttribute("bookshelfCount", bookService.countVisibleBooksForUser(userSession.getId()));
        });
        return "home";
    }
}
