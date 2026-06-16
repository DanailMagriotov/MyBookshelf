package app.web;

import app.model.dto.user.AdminUserDto;
import app.model.dto.user.UserSession;
import app.security.AuthenticationGuard;
import app.service.user.UserService;
import app.service.user.UserSessionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/users")
public class UsersController {

    private final UserService userService;
    private final UserSessionService userSessionService;
    private final AuthenticationGuard authenticationGuard;

    public UsersController(UserService userService,
                           UserSessionService userSessionService,
                           AuthenticationGuard authenticationGuard) {
        this.userService = userService;
        this.userSessionService = userSessionService;
        this.authenticationGuard = authenticationGuard;
    }

    @GetMapping
    public String users(@PageableDefault(size = UserService.USERS_PAGE_SIZE, sort = "username") Pageable pageable,
                        HttpSession session,
                        Model model) {
        requireAdmin(session);

        Page<AdminUserDto> userPage = userService.getUsers(pageable);

        model.addAttribute("users", userPage.getContent());
        model.addAttribute("page", userPage.getNumber());
        model.addAttribute("pageSize", UserService.USERS_PAGE_SIZE);
        model.addAttribute("totalPages", userPage.getTotalPages());
        model.addAttribute("totalUsers", userPage.getTotalElements());
        model.addAttribute("hasPrevious", userPage.hasPrevious());
        model.addAttribute("hasNext", userPage.hasNext());

        return "users";
    }

    private void requireAdmin(HttpSession session) {
        UserSession userSession = userSessionService.get(session).orElse(null);
        authenticationGuard.requireAdmin(userSession);
    }
}
