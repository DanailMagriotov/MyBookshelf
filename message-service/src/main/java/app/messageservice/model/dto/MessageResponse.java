package app.messageservice.model.dto;

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
public class MessageResponse {

    private UUID id;
    private UUID senderId;
    private UUID receiverId;
    private String content;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
    private boolean read;
}
