package app.model.dto.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageFormRequest {

    @NotBlank(message = "Recipient username is required")
    @Size(max = 50, message = "Username must be at most 50 characters")
    private String receiverUsername;

    @NotBlank(message = "Message subject is required")
    @Size(max = 30, message = "Message subject must be at most 30 characters")
    private String about;

    @NotBlank(message = "Message content is required")
    @Size(max = 1000, message = "Message must be at most 1000 characters")
    private String content;
}
