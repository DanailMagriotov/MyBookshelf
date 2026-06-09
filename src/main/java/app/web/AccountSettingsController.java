package app.web;

import app.exception.EmailAlreadyExistsException;
import app.exception.NotAuthenticatedException;
import app.mapper.user.UserMapper;
import app.model.dto.user.AccountSettingsUpdateRequest;
import app.model.dto.user.UserSession;
import app.model.entity.user.Region;
import app.repository.user.UserRepository;
import app.service.user.UserService;
import app.service.user.UserSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/account-settings")
public class AccountSettingsController {

    private final UserService userService;
    private final UserSessionService userSessionService;
    private final UserRepository userRepository;

    public AccountSettingsController(UserService userService,
                                       UserSessionService userSessionService,
                                       UserRepository userRepository) {
        this.userService = userService;
        this.userSessionService = userSessionService;
        this.userRepository = userRepository;
    }

    @ModelAttribute("regions")
    public Region[] regions() {
        return Region.values();
    }

    @GetMapping
    public String accountSettings(HttpSession session, Model model) {
        UserSession userSession = requireUserSession(session);

        if (!model.containsAttribute("accountSettingsRequest")) {
            userRepository.findById(userSession.getId())
                    .map(UserMapper::toAccountSettingsRequest)
                    .ifPresent(request -> model.addAttribute("accountSettingsRequest", request));
        }

        model.addAttribute("username", userSession.getUsername());
        return "account-settings";
    }

    @PostMapping
    public String updateAccountSettings(@Valid @ModelAttribute("accountSettingsRequest") AccountSettingsUpdateRequest request,
                                        BindingResult bindingResult,
                                        HttpSession session,
                                        Model model,
                                        RedirectAttributes redirectAttributes) {
        UserSession userSession = requireUserSession(session);
        model.addAttribute("username", userSession.getUsername());

        validatePasswordFields(request, bindingResult);

        if (bindingResult.hasErrors()) {
            return "account-settings";
        }

        try {
            UserSession updatedSession = userService.updateAccountSettings(userSession.getId(), request);
            userSessionService.save(session, updatedSession);
            redirectAttributes.addFlashAttribute("successMessage", "my profile updated successfully.");
            return "redirect:/account-settings";
        } catch (EmailAlreadyExistsException ex) {
            bindingResult.rejectValue("email", "email.exists", "Email already exists");
            return "account-settings";
        }
    }

    @PostMapping("/delete")
    public String deleteAccount(HttpSession session,
                                HttpServletRequest request,
                                HttpServletResponse response) {
        UserSession userSession = requireUserSession(session);
        userService.deleteAccount(userSession.getId());

        new SecurityContextLogoutHandler().logout(
                request,
                response,
                SecurityContextHolder.getContext().getAuthentication()
        );

        return "redirect:/";
    }

    private UserSession requireUserSession(HttpSession session) {
        return userSessionService.get(session)
                .orElseThrow(NotAuthenticatedException::new);
    }

    private void validatePasswordFields(AccountSettingsUpdateRequest request, BindingResult bindingResult) {
        boolean hasPassword = StringUtils.hasText(request.getPassword());
        boolean hasConfirmPassword = StringUtils.hasText(request.getConfirmPassword());

        if (hasPassword != hasConfirmPassword) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "Please confirm your new password");
            return;
        }

        if (!hasPassword) {
            return;
        }

        if (request.getPassword().length() < 6) {
            bindingResult.rejectValue("password", "password.size", "Password must be at least 6 characters");
        } else if (!request.getPassword().matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$")) {
            bindingResult.rejectValue("password", "password.pattern",
                    "Password must contain at least one uppercase letter, one lowercase letter, and one digit");
        } else if (!request.getPassword().equals(request.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "Passwords do not match");
        }
    }
}
