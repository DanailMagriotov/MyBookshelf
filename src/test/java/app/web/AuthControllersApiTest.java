package app.web;

import app.exception.EmailAlreadyExistsException;
import app.exception.UsernameAlreadyExistsException;
import app.security.UserSessionLoginSuccessHandler;
import app.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import static app.testsupport.WebTestSupport.standaloneWithPageable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class AuthControllersApiTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserSessionLoginSuccessHandler loginSuccessHandler;

    @Mock
    private UserService userService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private LoginController loginController;

    private MockMvc loginMockMvc;
    private MockMvc registerMockMvc;

    @BeforeEach
    void setUp() {
        loginMockMvc = standaloneWithPageable(loginController).build();
        registerMockMvc = standaloneWithPageable(new RegisterController(userService)).build();
    }

    @Test
    void loginPage_returnsLoginView() throws Exception {
        loginMockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void loginSubmit_withBlankFields_returnsLoginView() throws Exception {
        loginMockMvc.perform(post("/login/submit")
                        .param("username", "")
                        .param("password", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void loginSubmit_withInvalidCredentials_returnsLoginView() throws Exception {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        loginMockMvc.perform(post("/login/submit")
                        .param("username", "alice")
                        .param("password", "wrong"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void loginSubmit_withValidCredentials_invokesSuccessHandler() throws Exception {
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        doNothing().when(loginSuccessHandler).onAuthenticationSuccess(any(), any(), any());

        loginMockMvc.perform(post("/login/submit")
                        .param("username", "  alice  ")
                        .param("password", "Password1"))
                .andExpect(status().isOk());

        verify(loginSuccessHandler).onAuthenticationSuccess(any(), any(), any());
    }

    @Test
    void registerPage_returnsRegisterView() throws Exception {
        registerMockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    void register_withValidationErrors_returnsRegisterView() throws Exception {
        registerMockMvc.perform(post("/register")
                        .param("username", "ab")
                        .param("password", "weak")
                        .param("email", "bad-email")
                        .param("region", "SOFIA"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    void register_withValidData_redirectsToLogin() throws Exception {
        doNothing().when(userService).register(any());

        registerMockMvc.perform(post("/register")
                        .param("username", "newuser")
                        .param("password", "Password1")
                        .param("email", "new@example.com")
                        .param("region", "SOFIA"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("successMessage", "Registration successful. Please log in."));
    }

    @Test
    void register_withExistingUsername_returnsRegisterView() throws Exception {
        doThrow(new UsernameAlreadyExistsException()).when(userService).register(any());

        registerMockMvc.perform(post("/register")
                        .param("username", "existing")
                        .param("password", "Password1")
                        .param("email", "new@example.com")
                        .param("region", "SOFIA"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    void register_withExistingEmail_returnsRegisterView() throws Exception {
        doThrow(new EmailAlreadyExistsException()).when(userService).register(any());

        registerMockMvc.perform(post("/register")
                        .param("username", "newuser")
                        .param("password", "Password1")
                        .param("email", "taken@example.com")
                        .param("region", "SOFIA"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }
}
