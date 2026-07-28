package app.web;

import app.exception.EmailAlreadyExistsException;
import app.model.dto.user.AdminUserDto;
import app.model.dto.user.UserSession;
import app.model.entity.user.Region;
import app.model.entity.user.User;
import app.model.entity.user.UserRole;
import app.repository.user.UserRepository;
import app.security.AuthenticationGuard;
import app.service.user.UserService;
import app.service.user.UserSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static app.testsupport.WebTestSupport.adminSession;
import static app.testsupport.WebTestSupport.sessionWith;
import static app.testsupport.WebTestSupport.standaloneWithPageable;
import static app.testsupport.WebTestSupport.userSession;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
class ProfileAndUsersControllersApiTest {

    @Mock
    private UserService userService;

    @Mock
    private UserSessionService userSessionService;

    @Mock
    private UserRepository userRepository;

    private MockMvc profileMockMvc;
    private MockMvc usersMockMvc;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        profileMockMvc = standaloneWithPageable(
                new MyProfileController(userService, userSessionService, userRepository)).build();
        usersMockMvc = standaloneWithPageable(
                new UsersController(userService, userSessionService, new AuthenticationGuard())).build();
    }

    private UserSession authenticatedSession() {
        return userSession(userId, UserRole.USER);
    }

    @Test
    void myProfilePage_returnsProfileView() throws Exception {
        var session = authenticatedSession();
        var user = User.builder()
                .id(userId)
                .username("alice")
                .email("alice@example.com")
                .region(Region.SOFIA)
                .role(UserRole.USER)
                .build();

        when(userSessionService.get(any())).thenReturn(Optional.of(session));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        profileMockMvc.perform(get("/my-profile").session(sessionWith(session)))
                .andExpect(status().isOk())
                .andExpect(view().name("my-profile"));
    }

    @Test
    void updateMyProfile_withValidData_redirectsToProfile() throws Exception {
        var session = authenticatedSession();
        var updatedSession = UserSession.builder()
                .id(userId)
                .username("alice")
                .email("new@example.com")
                .role(UserRole.USER)
                .region(Region.SOFIA)
                .build();

        when(userSessionService.get(any())).thenReturn(Optional.of(session));
        when(userService.updateMyProfile(eq(userId), any())).thenReturn(updatedSession);
        doNothing().when(userSessionService).save(any(), eq(updatedSession));

        profileMockMvc.perform(post("/my-profile")
                        .session(sessionWith(session))
                        .param("email", "new@example.com")
                        .param("region", "SOFIA"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/my-profile"))
                .andExpect(flash().attribute("successMessage", "my profile updated successfully."));
    }

    @Test
    void updateMyProfile_withExistingEmail_returnsProfileView() throws Exception {
        var session = authenticatedSession();

        when(userSessionService.get(any())).thenReturn(Optional.of(session));
        doThrow(new EmailAlreadyExistsException()).when(userService).updateMyProfile(eq(userId), any());

        profileMockMvc.perform(post("/my-profile")
                        .session(sessionWith(session))
                        .param("email", "taken@example.com")
                        .param("region", "SOFIA"))
                .andExpect(status().isOk())
                .andExpect(view().name("my-profile"));
    }

    @Test
    void updateMyProfile_withMismatchedPasswords_returnsProfileView() throws Exception {
        var session = authenticatedSession();

        when(userSessionService.get(any())).thenReturn(Optional.of(session));

        profileMockMvc.perform(post("/my-profile")
                        .session(sessionWith(session))
                        .param("email", "alice@example.com")
                        .param("region", "SOFIA")
                        .param("password", "Password1")
                        .param("confirmPassword", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("my-profile"));
    }

    @Test
    void deleteAccount_forRegularUser_redirectsToIndex() throws Exception {
        var session = authenticatedSession();

        when(userSessionService.get(any())).thenReturn(Optional.of(session));
        doNothing().when(userService).deleteAccount(userId);

        profileMockMvc.perform(post("/my-profile/delete").session(sessionWith(session)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void deleteAccount_forAdmin_redirectsToHome() throws Exception {
        var session = userSession(userId, UserRole.ADMIN);

        when(userSessionService.get(any())).thenReturn(Optional.of(session));

        profileMockMvc.perform(post("/my-profile/delete").session(sessionWith(session)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));
    }

    @Test
    void usersPage_returnsUsersView() throws Exception {
        var session = adminSession();
        var adminDto = AdminUserDto.builder().id(UUID.randomUUID()).username("bob").build();
        var page = new PageImpl<>(List.of(adminDto), PageRequest.of(0, UserService.USERS_PAGE_SIZE), 1);

        when(userSessionService.get(any())).thenReturn(Optional.of(session));
        when(userService.getUsers(any())).thenReturn(page);

        usersMockMvc.perform(get("/users").session(sessionWith(session)))
                .andExpect(status().isOk())
                .andExpect(view().name("users"));
    }

    @Test
    void deleteUser_whenAllowed_redirectsWithSuccessMessage() throws Exception {
        var session = adminSession();
        var targetUserId = UUID.randomUUID();

        when(userSessionService.get(any())).thenReturn(Optional.of(session));
        when(userService.deleteUserByAdmin(targetUserId)).thenReturn(true);

        usersMockMvc.perform(post("/users/delete/{userId}", targetUserId)
                        .session(sessionWith(session))
                        .param("page", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users?page=0&size=6&sort=username%2Casc"))
                .andExpect(flash().attribute("successMessage", "User account deleted successfully."));
    }

    @Test
    void deleteUser_whenNotAllowed_redirectsWithErrorMessage() throws Exception {
        var session = adminSession();
        var targetUserId = UUID.randomUUID();

        when(userSessionService.get(any())).thenReturn(Optional.of(session));
        when(userService.deleteUserByAdmin(targetUserId)).thenReturn(false);

        usersMockMvc.perform(post("/users/delete/{userId}", targetUserId)
                        .session(sessionWith(session)))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("errorMessage", "This account cannot be deleted."));

        verify(userService).deleteUserByAdmin(targetUserId);
    }
}
