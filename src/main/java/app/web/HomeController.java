package app.web;

import app.service.user.UserSessionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final UserSessionService userSessionService;

    public HomeController(UserSessionService userSessionService) {
        this.userSessionService = userSessionService;
    }

    @GetMapping("/home")
    public String home(HttpSession session, Model model) {
        userSessionService.get(session).ifPresent(userSession -> {
            model.addAttribute("userSession", userSession);
            model.addAttribute("username", userSession.getUsername());
        });
        return "home";
    }
}
