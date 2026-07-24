package app.messageservice.mapper;

import app.messageservice.model.dto.MessageResponse;
import app.messageservice.model.entity.Message;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class MessageMapper {

    public static MessageResponse toMessageResponse(Message message) {
        if (message == null) {
            return null;
        }

        return MessageResponse.builder()
                .id(message.getId())
                .senderId(message.getSenderId())
                .receiverId(message.getReceiverId())
                .about(message.getAbout())
                .content(message.getContent())
                .sentAt(message.getSentAt())
                .readAt(message.getReadAt())
                .read(message.isRead())
                .build();
    }
}
