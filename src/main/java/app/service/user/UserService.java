package app.service.user;

import app.event.UserRoleChangedEvent;
import app.exception.EmailAlreadyExistsException;
import app.exception.NotAuthenticatedException;
import app.exception.UsernameAlreadyExistsException;
import app.mapper.user.UserMapper;
import app.model.dto.user.AdminUserDto;
import app.model.dto.user.MyProfileUpdateRequest;
import app.model.dto.user.UserRegRequest;
import app.model.dto.user.UserSession;
import app.model.entity.user.User;
import app.model.entity.user.UserRole;
import app.repository.book.BookRepository;
import app.repository.booktransfer.BookTransferRepository;
import app.repository.user.UserRepository;
import app.validation.EntityValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    public static final int USERS_PAGE_SIZE = 6;

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final BookTransferRepository bookTransferRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityValidator entityValidator;
    private final ApplicationEventPublisher eventPublisher;

    public UserService(UserRepository userRepository,
                       BookRepository bookRepository,
                       BookTransferRepository bookTransferRepository,
                       PasswordEncoder passwordEncoder,
                       EntityValidator entityValidator,
                       ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.bookTransferRepository = bookTransferRepository;
        this.passwordEncoder = passwordEncoder;
        this.entityValidator = entityValidator;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void register(UserRegRequest userRegRequest) {
        if (userRepository.existsByUsername(userRegRequest.getUsername())) {
            throw new UsernameAlreadyExistsException();
        }

        if (userRepository.existsByEmail(userRegRequest.getEmail())) {
            throw new EmailAlreadyExistsException();
        }

        String encodedPassword = passwordEncoder.encode(userRegRequest.getPassword());
        UserRole role = userRepository.count() == 0 ? UserRole.MASTER_ADMIN : UserRole.USER;
        User userEntity = UserMapper.toUserEntity(userRegRequest, encodedPassword, role);
        entityValidator.validate(userEntity);
        userRepository.save(userEntity);
        log.info("Registered user '{}' with role {}", userRegRequest.getUsername(), role);
    }

    @Transactional
    public UserSession updateMyProfile(UUID userId, MyProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(NotAuthenticatedException::new);

        if (userRepository.existsByEmailAndIdNot(request.getEmail(), userId)) {
            throw new EmailAlreadyExistsException();
        }

        user.setFirstName(trimToNull(request.getFirstName()));
        user.setLastName(trimToNull(request.getLastName()));
        user.setEmail(request.getEmail());
        user.setRegion(request.getRegion());

        if (StringUtils.hasText(request.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        entityValidator.validate(user);
        User savedUser = userRepository.save(user);
        log.info("User {} updated profile", userId);
        return UserMapper.toUserSession(savedUser);
    }

    @Transactional
    public void deleteAccount(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotAuthenticatedException();
        }

        bookTransferRepository.deleteByBook_Owner_Id(userId);
        bookTransferRepository.deleteBySender_Id(userId);
        bookTransferRepository.deleteByReceiver_Id(userId);
        bookRepository.deleteByOwner_Id(userId);
        userRepository.deleteById(userId);
        log.info("Deleted user account {}", userId);
    }

    @Transactional
    public boolean deleteUserByAdmin(UUID userId) {
        return userRepository.findById(userId)
                .filter(user -> user.getRole() == UserRole.USER)
                .map(user -> {
                    deleteAccount(userId);
                    return true;
                })
                .orElseGet(() -> {
                    log.warn("Admin delete rejected for user {}", userId);
                    return false;
                });
    }

    @Transactional
    public boolean changeUserRoleByAdmin(UUID targetUserId,
                                         UserRole newRole,
                                         UUID actingAdminId,
                                         UserRole actingAdminRole) {
        if (!actingAdminRole.isAdmin()) {
            log.warn("Role change rejected: actor {} is not admin", actingAdminId);
            return false;
        }

        if (actingAdminId.equals(targetUserId)) {
            log.warn("Role change rejected: user {} attempted to change own role", actingAdminId);
            return false;
        }

        if (newRole == UserRole.MASTER_ADMIN) {
            log.warn("Role change rejected: cannot assign MASTER_ADMIN via admin UI");
            return false;
        }

        User targetUser = userRepository.findById(targetUserId).orElse(null);
        if (targetUser == null) {
            log.warn("Role change rejected: user {} not found", targetUserId);
            return false;
        }

        if (SystemUserService.SYSTEM_USERNAME.equals(targetUser.getUsername())) {
            log.warn("Role change rejected: system user cannot be changed");
            return false;
        }

        if (targetUser.getRole() == UserRole.MASTER_ADMIN) {
            log.warn("Role change rejected: master admin role cannot be changed");
            return false;
        }

        if (targetUser.getRole() == newRole) {
            log.warn("Role change rejected: user {} already has role {}", targetUserId, newRole);
            return false;
        }

        if (newRole == UserRole.ADMIN) {
            if (targetUser.getRole() != UserRole.USER) {
                log.warn("Role change rejected: only USER accounts can be promoted");
                return false;
            }
        } else if (newRole == UserRole.USER) {
            if (targetUser.getRole() != UserRole.ADMIN) {
                log.warn("Role change rejected: only ADMIN accounts can be demoted to USER");
                return false;
            }
            if (actingAdminRole != UserRole.MASTER_ADMIN) {
                log.warn("Role change rejected: only master admin can demote admins");
                return false;
            }
        } else {
            log.warn("Role change rejected: unsupported target role {}", newRole);
            return false;
        }

        UserRole previousRole = targetUser.getRole();
        targetUser.setRole(newRole);
        userRepository.save(targetUser);
        log.info("Admin {} changed role of user {} from {} to {}",
                actingAdminId, targetUserId, previousRole, newRole);
        eventPublisher.publishEvent(new UserRoleChangedEvent(targetUserId, newRole));
        return true;
    }

    public boolean isUnknownUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return true;
        }
        return !userRepository.existsByUsername(username.trim());
    }

    public boolean matchesCurrentPassword(UUID userId, String rawPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(NotAuthenticatedException::new);
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    public Page<AdminUserDto> getUsers(Pageable pageable) {
        int pageNumber = Math.max(pageable.getPageNumber(), 0);
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.by("username").ascending();
        PageRequest pageRequest = PageRequest.of(pageNumber, USERS_PAGE_SIZE, sort);

        return userRepository.findByUsernameNot(SystemUserService.SYSTEM_USERNAME, pageRequest)
                .map(user -> UserMapper.toAdminUserDto(user, bookRepository.countByOwner_Id(user.getId())));
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
