package app.mapper.user;

import app.model.dto.user.AdminUserDto;
import app.model.dto.user.MyProfileUpdateRequest;
import app.model.dto.user.UserRegRequest;
import app.model.dto.user.UserSession;
import app.model.entity.user.Region;
import app.model.entity.user.User;
import app.model.entity.user.UserRole;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    @Test
    void toUserEntity_mapsRegistrationRequest() {
        UserRegRequest request = UserRegRequest.builder()
                .username("alice")
                .email("alice@example.com")
                .region(Region.SOFIA)
                .build();

        User user = UserMapper.toUserEntity(request, "encoded", UserRole.USER);

        assertThat(user.getUsername()).isEqualTo("alice");
        assertThat(user.getPassword()).isEqualTo("encoded");
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        assertThat(user.getBooks()).isEmpty();
    }

    @Test
    void toUserEntity_returnsNullForNullRequest() {
        assertThat(UserMapper.toUserEntity(null, "encoded", UserRole.USER)).isNull();
    }

    @Test
    void toMyProfileUpdateRequest_mapsUserFields() {
        User user = User.builder()
                .firstName("Ana")
                .lastName("Ivanova")
                .email("ana@example.com")
                .region(Region.VARNA)
                .build();

        MyProfileUpdateRequest request = UserMapper.toMyProfileUpdateRequest(user);

        assertThat(request.getFirstName()).isEqualTo("Ana");
        assertThat(request.getEmail()).isEqualTo("ana@example.com");
    }

    @Test
    void toUserSession_mapsUserFields() {
        UUID id = UUID.randomUUID();
        User user = User.builder()
                .id(id)
                .username("alice")
                .email("alice@example.com")
                .role(UserRole.ADMIN)
                .region(Region.SOFIA)
                .firstName("Alice")
                .lastName("Admin")
                .build();

        UserSession session = UserMapper.toUserSession(user);

        assertThat(session.getId()).isEqualTo(id);
        assertThat(session.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(session.getUsername()).isEqualTo("alice");
    }

    @Test
    void toMyProfileUpdateRequest_returnsNullForNullUser() {
        assertThat(UserMapper.toMyProfileUpdateRequest(null)).isNull();
    }

    @Test
    void toUserSession_returnsNullForNullUser() {
        assertThat(UserMapper.toUserSession(null)).isNull();
    }

    @Test
    void toAdminUserDto_returnsNullForNullUser() {
        assertThat(UserMapper.toAdminUserDto(null, 0L)).isNull();
    }

    @Test
    void toAdminUserDto_includesBookCount() {
        UUID id = UUID.randomUUID();
        User user = User.builder()
                .id(id)
                .username("alice")
                .role(UserRole.USER)
                .region(Region.SOFIA)
                .email("alice@example.com")
                .build();

        AdminUserDto dto = UserMapper.toAdminUserDto(user, 7L);

        assertThat(dto.getBookCount()).isEqualTo(7L);
        assertThat(dto.getUsername()).isEqualTo("alice");
    }
}
