package app.model.dto.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageViewDto {

    private UUID id;
    private String senderUsername;
    private String recipientUsername;
    private String about;
    private String content;
    private LocalDateTime sentAt;
    private boolean read;
    private boolean markableAsRead;
    private boolean deletable;
}
