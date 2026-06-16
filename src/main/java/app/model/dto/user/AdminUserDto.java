package app.model.dto.user;

import app.model.entity.user.Region;
import app.model.entity.user.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDto {

    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private UserRole role;
    private Region region;
    private long bookCount;
}
