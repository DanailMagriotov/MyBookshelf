package app.web;

import app.model.dto.user.AdminUserDto;
import app.model.dto.user.UserSession;
import app.model.entity.user.UserRole;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

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
        UserSession userSession = requireAdminSession(session);

        Page<AdminUserDto> userPage = userService.getUsers(pageable);

        model.addAttribute("users", userPage.getContent());
        model.addAttribute("page", userPage.getNumber());
        model.addAttribute("pageSize", UserService.USERS_PAGE_SIZE);
        model.addAttribute("totalPages", userPage.getTotalPages());
        model.addAttribute("totalUsers", userPage.getTotalElements());
        model.addAttribute("hasPrevious", userPage.hasPrevious());
        model.addAttribute("hasNext", userPage.hasNext());
        model.addAttribute("isMasterAdmin", userSession.getRole() == UserRole.MASTER_ADMIN);

        return "users";
    }

    @PostMapping("/delete/{userId}")
    public String deleteUser(@PathVariable UUID userId,
                             @RequestParam(defaultValue = "0") int page,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        requireAdminSession(session);

        if (userService.deleteUserByAdmin(userId)) {
            redirectAttributes.addFlashAttribute("successMessage", "User account deleted successfully.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "This account cannot be deleted.");
        }

        return redirectToUsers(page, redirectAttributes);
    }

    @PostMapping("/role/{userId}")
    public String changeUserRole(@PathVariable UUID userId,
                                 @RequestParam UserRole role,
                                 @RequestParam(defaultValue = "0") int page,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        UserSession userSession = requireAdminSession(session);

        if (userService.changeUserRoleByAdmin(userId, role, userSession.getId(), userSession.getRole())) {
            redirectAttributes.addFlashAttribute("successMessage", "User role updated successfully.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "This role change is not allowed.");
        }

        return redirectToUsers(page, redirectAttributes);
    }

    private UserSession requireAdminSession(HttpSession session) {
        UserSession userSession = userSessionService.get(session).orElse(null);
        authenticationGuard.requireAdmin(userSession);
        return userSession;
    }

    private String redirectToUsers(int page, RedirectAttributes redirectAttributes) {
        redirectAttributes.addAttribute("page", page);
        redirectAttributes.addAttribute("size", UserService.USERS_PAGE_SIZE);
        redirectAttributes.addAttribute("sort", "username,asc");
        return "redirect:/users";
    }
}
