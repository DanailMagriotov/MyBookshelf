package app.web;

import app.security.AuthenticationGuard;
import app.service.user.UserSessionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/home/actions")
public class HomeActionController {

    private final AuthenticationGuard authenticationGuard;
    private final UserSessionService userSessionService;

    public HomeActionController(AuthenticationGuard authenticationGuard,
                                  UserSessionService userSessionService) {
        this.authenticationGuard = authenticationGuard;
        this.userSessionService = userSessionService;
    }

    @GetMapping("/bookshelf")
    public String myBookshelf(HttpSession session) {
        authenticationGuard.requireAuthenticated(userSessionService.get(session).orElse(null));
        return "redirect:/my-bookshelf";
    }

    @GetMapping("/add-book")
    public String addBook(HttpSession session) {
        authenticationGuard.requireAuthenticated(userSessionService.get(session).orElse(null));
        return "redirect:/add-book";
    }

    @GetMapping("/send-book")
    public String sendBook(HttpSession session) {
        authenticationGuard.requireAuthenticated(userSessionService.get(session).orElse(null));
        return "redirect:/send-book";
    }

    @GetMapping("/messages")
    public String messages(HttpSession session) {
        authenticationGuard.requireAuthenticated(userSessionService.get(session).orElse(null));
        return "redirect:/home";
    }

    @GetMapping("/my-profile")
    public String myProfile(HttpSession session) {
        authenticationGuard.requireAuthenticated(userSessionService.get(session).orElse(null));
        return "redirect:/my-profile";
    }

    @GetMapping("/users")
    public String users(HttpSession session) {
        authenticationGuard.requireAdmin(userSessionService.get(session).orElse(null));
        return "redirect:/home";
    }
}
