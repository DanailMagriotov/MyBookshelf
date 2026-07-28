package app.service.user;

import app.model.entity.user.Region;
import app.model.entity.user.User;
import app.model.entity.user.UserRole;
import app.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private SystemUserService systemUserService;

    @Test
    void getSystemUserId_returnsExistingUser() {
        UUID systemUserId = UUID.randomUUID();
        User systemUser = User.builder()
                .id(systemUserId)
                .username(SystemUserService.SYSTEM_USERNAME)
                .build();

        when(userRepository.findByUsername(SystemUserService.SYSTEM_USERNAME))
                .thenReturn(Optional.of(systemUser));

        assertThat(systemUserService.getSystemUserId()).isEqualTo(systemUserId);
    }

    @Test
    void getSystemUserId_createsUserWhenMissing() {
        UUID systemUserId = UUID.randomUUID();
        when(userRepository.findByUsername(SystemUserService.SYSTEM_USERNAME)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any(String.class))).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(systemUserId);
            return user;
        });

        UUID result = systemUserService.getSystemUserId();

        assertThat(result).isEqualTo(systemUserId);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo(SystemUserService.SYSTEM_USERNAME);
        assertThat(captor.getValue().getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(captor.getValue().getRegion()).isEqualTo(Region.SOFIA);
    }
}
