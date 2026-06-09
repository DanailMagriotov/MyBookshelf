package app.service.user;

import app.exception.EmailAlreadyExistsException;
import app.exception.NotAuthenticatedException;
import app.exception.UsernameAlreadyExistsException;
import app.mapper.user.UserMapper;
import app.model.dto.user.MyProfileUpdateRequest;
import app.model.dto.user.UserRegRequest;
import app.model.dto.user.UserSession;
import app.model.entity.user.User;
import app.repository.book.BookRepository;
import app.repository.booktransfer.BookTransferRepository;
import app.repository.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final BookTransferRepository bookTransferRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       BookRepository bookRepository,
                       BookTransferRepository bookTransferRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.bookTransferRepository = bookTransferRepository;
        this.passwordEncoder = passwordEncoder;
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
        User userEntity = UserMapper.toUserEntity(userRegRequest, encodedPassword);
        userRepository.save(userEntity);
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

        User savedUser = userRepository.save(user);
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
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
