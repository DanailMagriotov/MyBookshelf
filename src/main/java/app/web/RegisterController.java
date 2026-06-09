package app.web;

import app.exception.UsernameAlreadyExistsException;
import app.model.dto.user.UserRegRequest;
import app.model.entity.user.Region;
import app.service.user.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RegisterController {

    private final UserService userService;

    public RegisterController(UserService userService) {
        this.userService = userService;
    }

    @ModelAttribute("userRegRequest")
    public UserRegRequest userRegRequest() {
        return UserRegRequest.builder().build();
    }

    @GetMapping("/register")
    public String registerForm() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("userRegRequest") UserRegRequest userRegRequest,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "register";
        }

        try {
            userService.register(userRegRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Registration successful. Please log in.");
            return "redirect:/login";
        } catch (UsernameAlreadyExistsException ex) {
            bindingResult.rejectValue("username", "username.exists", "Username already exists");
            return "register";
        }
    }

    @ModelAttribute("regions")
    public Region[] regions() {
        return Region.values();
    }
}
