package app.mapper.user;

import app.mapper.book.BookMapper;
import app.model.dto.book.BookDto;
import app.model.dto.user.AccountSettingsUpdateRequest;
import app.model.dto.user.UserDto;
import app.model.dto.user.UserRegRequest;
import app.model.dto.user.UserSession;
import app.model.entity.user.User;
import app.model.entity.user.UserRole;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;


@NoArgsConstructor
public class UserMapper {

    public static User toUserEntity(UserRegRequest userRegRequest, String encodedPassword) {
        if (userRegRequest == null) {
            return null;
        }
        return User.builder()
                .username(userRegRequest.getUsername())
                .password(encodedPassword)
                .email(userRegRequest.getEmail())
                .region(userRegRequest.getRegion())
                .role(UserRole.USER)
                .books(new ArrayList<>())
                .build();
    }

    public static UserDto toUserDto(User user) {
        if (user == null) {
            return null;
        }

        List<BookDto> bookDtoList = user.getBooks() == null
                ? List.of()
                : user.getBooks().stream().map(BookMapper::toBookDto).toList();

        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .region(user.getRegion())
                .role(user.getRole())
                .books(bookDtoList)
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
    }

    public static AccountSettingsUpdateRequest toAccountSettingsRequest(User user) {
        if (user == null) {
            return null;
        }

        return AccountSettingsUpdateRequest.builder()
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
