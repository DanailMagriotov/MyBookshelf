package app.model.dto.user;

import app.model.entity.user.Region;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegRequest {

    private String username;
    private String password;
    private String email;
    private Region region;
}
