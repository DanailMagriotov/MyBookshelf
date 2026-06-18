package app.web;

import app.model.dto.user.LoginRequest;
import app.security.UserSessionLoginSuccessHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.io.IOException;

@Controller
public class LoginController {

    private final AuthenticationManager authenticationManager;
    private final UserSessionLoginSuccessHandler loginSuccessHandler;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public LoginController(AuthenticationManager authenticationManager,
                           UserSessionLoginSuccessHandler loginSuccessHandler) {
        this.authenticationManager = authenticationManager;
        this.loginSuccessHandler = loginSuccessHandler;
    }

    @ModelAttribute("loginRequest")
    public LoginRequest loginRequest() {
        return LoginRequest.builder().build();
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @PostMapping("/login/submit")
    public String loginSubmit(@Valid @ModelAttribute("loginRequest") LoginRequest loginRequest,
                              BindingResult bindingResult,
                              HttpServletRequest request,
                              HttpServletResponse response) throws IOException {
        if (bindingResult.hasErrors()) {
            return "login";
        }

        String username = loginRequest.getUsername().trim();
        loginRequest.setUsername(username);

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, loginRequest.getPassword())
            );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, request, response);

            loginSuccessHandler.onAuthenticationSuccess(request, response, authentication);
            return null;
        } catch (AuthenticationException ex) {
            bindingResult.rejectValue("password", "login.invalid", "Invalid username or password.");
            return "login";
        }
    }
}
