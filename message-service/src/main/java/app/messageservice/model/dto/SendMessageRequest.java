package app.messageservice.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {

    @NotNull(message = "Sender id is required")
    private UUID senderId;

    @NotNull(message = "Receiver id is required")
    private UUID receiverId;

    @NotBlank(message = "Message content is required")
    @Size(max = 1000, message = "Message must be at most 1000 characters")
    private String content;
}
