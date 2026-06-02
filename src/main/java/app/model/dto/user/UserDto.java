package app.model.dto.user;

import app.model.dto.book.BookDto;
import app.model.entity.user.Region;
import app.model.entity.user.UserRole;
import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class UserDto {

    private UUID id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private UserRole role;
    private Region region;
    private List<BookDto> books;
}
