package app.service.user;

import app.model.entity.user.Region;
import app.model.entity.user.User;
import app.model.entity.user.UserRole;
import app.repository.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SystemUserService {

    private static final Logger log = LoggerFactory.getLogger(SystemUserService.class);

    public static final String SYSTEM_USERNAME = "System message";
    private static final String SYSTEM_EMAIL = "system-message@mybookshelf.local";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SystemUserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UUID getSystemUserId() {
        return userRepository.findByUsername(SYSTEM_USERNAME)
                .map(User::getId)
                .orElseGet(this::createSystemUser);
    }

    private UUID createSystemUser() {
        User systemUser = User.builder()
                .username(SYSTEM_USERNAME)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .email(SYSTEM_EMAIL)
                .role(UserRole.ADMIN)
                .region(Region.SOFIA)
                .build();

        UUID systemUserId = userRepository.save(systemUser).getId();
        log.info("Created system user with id {}", systemUserId);
        return systemUserId;
    }
}
