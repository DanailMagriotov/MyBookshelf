package app.service.user;

import app.exception.UsernameAlreadyExistsException;
import app.mapper.user.UserMapper;
import app.model.dto.user.UserDto;
import app.model.dto.user.UserRegRequest;
import app.model.entity.user.User;
import app.repository.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserDto register(UserRegRequest userRegRequest) {
        if (userRepository.existsByUsername(userRegRequest.getUsername())) {
            throw new UsernameAlreadyExistsException();
        }

        String encodedPassword = passwordEncoder.encode(userRegRequest.getPassword());
        User userEntity = UserMapper.toUserEntity(userRegRequest, encodedPassword);
        User savedUser = userRepository.save(userEntity);

        return UserMapper.toUserDto(savedUser);
    }
}
