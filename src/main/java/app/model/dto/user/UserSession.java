package app.model.dto.user;

import app.model.entity.user.Region;
import app.model.entity.user.UserRole;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Data
@Builder
public class UserSession implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private UUID id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private UserRole role;
    private Region region;
}
