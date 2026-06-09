package app.mapper.user;

import app.model.dto.user.MyProfileUpdateRequest;
import app.model.dto.user.UserRegRequest;
import app.model.dto.user.UserSession;
import app.model.entity.user.User;
import app.model.entity.user.UserRole;
import lombok.NoArgsConstructor;

import java.util.ArrayList;


@NoArgsConstructor
public class UserMapper {

    public static User toUserEntity(UserRegRequest userRegRequest, String encodedPassword, UserRole role) {
        if (userRegRequest == null) {
            return null;
        }
        return User.builder()
                .username(userRegRequest.getUsername())
                .password(encodedPassword)
                .email(userRegRequest.getEmail())
                .region(userRegRequest.getRegion())
                .role(role)
                .books(new ArrayList<>())
                .build();
    }

    public static MyProfileUpdateRequest toMyProfileUpdateRequest(User user) {
        if (user == null) {
            return null;
        }

        return MyProfileUpdateRequest.builder()
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .region(user.getRegion())
                .build();
    }

    public static UserSession toUserSession(User user) {
        if (user == null) {
            return null;
        }

        return UserSession.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .region(user.getRegion())
                .role(user.getRole())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
    }
}
