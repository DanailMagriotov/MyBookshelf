package app.service.user;

import app.exception.EmailAlreadyExistsException;
import app.exception.NotAuthenticatedException;
import app.exception.UsernameAlreadyExistsException;
import app.model.dto.user.MyProfileUpdateRequest;
import app.model.dto.user.UserRegRequest;
import app.model.dto.user.UserSession;
import app.model.entity.user.Region;
import app.model.entity.user.User;
import app.model.entity.user.UserRole;
import app.repository.book.BookRepository;
import app.repository.booktransfer.BookTransferRepository;
import app.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookTransferRepository bookTransferRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void register_firstUserBecomesAdmin() {
        UserRegRequest request = UserRegRequest.builder()
                .username("alice")
                .email("alice@example.com")
                .password("password")
                .region(Region.SOFIA)
                .build();

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(userRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode("password")).thenReturn("encoded");

        userService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(captor.getValue().getPassword()).isEqualTo("encoded");
    }

    @Test
    void register_secondUserBecomesUserRole() {
        UserRegRequest request = UserRegRequest.builder()
                .username("bob")
                .email("bob@example.com")
                .password("password")
                .region(Region.VARNA)
                .build();

        when(userRepository.existsByUsername("bob")).thenReturn(false);
        when(userRepository.existsByEmail("bob@example.com")).thenReturn(false);
        when(userRepository.count()).thenReturn(1L);
        when(passwordEncoder.encode("password")).thenReturn("encoded");

        userService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    void register_throwsWhenUsernameExists() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(UserRegRequest.builder()
                .username("alice")
                .email("alice@example.com")
                .password("password")
                .region(Region.SOFIA)
                .build()))
                .isInstanceOf(UsernameAlreadyExistsException.class);
    }

    @Test
    void register_throwsWhenEmailExists() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(UserRegRequest.builder()
                .username("alice")
                .email("alice@example.com")
                .password("password")
                .region(Region.SOFIA)
                .build()))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void updateMyProfile_updatesFieldsAndReturnsSession() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, UserRole.USER);
        MyProfileUpdateRequest request = MyProfileUpdateRequest.builder()
                .firstName(" Ana ")
                .lastName(" Ivanova ")
                .email("new@example.com")
                .region(Region.PLOVDIV)
                .password("new-password")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndIdNot("new@example.com", userId)).thenReturn(false);
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-new");
        when(userRepository.save(user)).thenReturn(user);

        UserSession session = userService.updateMyProfile(userId, request);

        assertThat(user.getFirstName()).isEqualTo("Ana");
        assertThat(user.getEmail()).isEqualTo("new@example.com");
        assertThat(user.getPassword()).isEqualTo("encoded-new");
        assertThat(session.getUsername()).isEqualTo("alice");
    }

    @Test
    void deleteAccount_removesRelatedData() {
        UUID userId = UUID.randomUUID();
        when(userRepository.existsById(userId)).thenReturn(true);

        userService.deleteAccount(userId);

        verify(bookTransferRepository).deleteByBook_Owner_Id(userId);
        verify(bookTransferRepository).deleteBySender_Id(userId);
        verify(bookTransferRepository).deleteByReceiver_Id(userId);
        verify(bookRepository).deleteByOwner_Id(userId);
        verify(userRepository).deleteById(userId);
    }

    @Test
    void deleteAccount_throwsWhenUserMissing() {
        UUID userId = UUID.randomUUID();
        when(userRepository.existsById(userId)).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteAccount(userId))
                .isInstanceOf(NotAuthenticatedException.class);
    }

    @Test
    void deleteUserByAdmin_deletesOnlyRegularUsers() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId, UserRole.USER)));
        when(userRepository.existsById(userId)).thenReturn(true);

        assertThat(userService.deleteUserByAdmin(userId)).isTrue();
        verify(userRepository).deleteById(userId);
    }

    @Test
    void deleteUserByAdmin_returnsFalseForAdmin() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId, UserRole.ADMIN)));

        assertThat(userService.deleteUserByAdmin(userId)).isFalse();
        verify(userRepository, never()).deleteById(userId);
    }

    @Test
    void isUnknownUsername_returnsTrueForBlankOrMissingUser() {
        assertThat(userService.isUnknownUsername("")).isTrue();
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        assertThat(userService.isUnknownUsername(" alice ")).isTrue();
    }

    @Test
    void matchesCurrentPassword_delegatesToEncoder() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, UserRole.USER);
        user.setPassword("encoded");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("raw", "encoded")).thenReturn(true);

        assertThat(userService.matchesCurrentPassword(userId, "raw")).isTrue();
    }

    @Test
    void isUnknownUsername_returnsFalseForExistingUser() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThat(userService.isUnknownUsername("alice")).isFalse();
    }

    @Test
    void updateMyProfile_keepsPasswordWhenBlank() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, UserRole.USER);
        user.setPassword("existing-encoded");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndIdNot("new@example.com", userId)).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);

        userService.updateMyProfile(userId, MyProfileUpdateRequest.builder()
                .email("new@example.com")
                .region(Region.VARNA)
                .build());

        assertThat(user.getPassword()).isEqualTo("existing-encoded");
    }

    @Test
    void updateMyProfile_throwsWhenEmailExists() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId, UserRole.USER)));
        when(userRepository.existsByEmailAndIdNot("taken@example.com", userId)).thenReturn(true);

        assertThatThrownBy(() -> userService.updateMyProfile(userId, MyProfileUpdateRequest.builder()
                .email("taken@example.com")
                .region(Region.SOFIA)
                .build()))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void matchesCurrentPassword_returnsFalseWhenMismatch() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, UserRole.USER);
        user.setPassword("encoded");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThat(userService.matchesCurrentPassword(userId, "wrong")).isFalse();
    }

    @Test
    void getUsers_returnsAdminDtosWithBookCount() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, UserRole.USER);
        Page<User> page = new PageImpl<>(List.of(user));

        when(userRepository.findAll(any(PageRequest.class))).thenReturn(page);
        when(bookRepository.countByOwner_Id(userId)).thenReturn(4L);

        Page<?> result = userService.getUsers(PageRequest.of(0, 6));

        assertThat(result.getTotalElements()).isOne();
    }

    private static User user(UUID id, UserRole role) {
        return User.builder()
                .id(id)
                .username("alice")
                .password("encoded")
                .email("alice@example.com")
                .role(role)
                .region(Region.SOFIA)
                .build();
    }
}
