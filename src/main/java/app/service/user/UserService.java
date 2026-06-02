package app.service.user;

import app.mapper.user.UserMapper;
import app.model.dto.user.UserDto;
import app.model.dto.user.UserRegRequest;
import app.model.entity.user.User;
import app.repository.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserDto register (UserRegRequest userRegRequest) {

        userRepository.findByUsername(userRegRequest.getUsername()).ifPresent(user -> {
            throw new RuntimeException("User with this username already exists");
        });

        String encodedPassword = passwordEncoder.encode(userRegRequest.getPassword());
        userRegRequest.setPassword(encodedPassword);
        User userEntity = UserMapper.toUserEntity(userRegRequest);
        userRepository.save(userEntity);

        return UserMapper.toUserDto(userEntity);

    }
}
