package app.model.dto.user;

import app.model.entity.user.Region;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class UserRegRequest {

    private String username;
    private String password;
    private String email;
    private Region region;
}
