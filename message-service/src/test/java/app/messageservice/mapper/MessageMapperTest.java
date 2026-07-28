package app.messageservice.mapper;

import app.messageservice.model.dto.MessageResponse;
import app.messageservice.model.entity.Message;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MessageMapperTest {

    @Test
    void toMessageResponse_returnsNullForNullInput() {
        assertThat(MessageMapper.toMessageResponse(null)).isNull();
    }

    @Test
    void toMessageResponse_mapsFields() {
        UUID id = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        LocalDateTime sentAt = LocalDateTime.now();
        Message message = Message.builder()
                .id(id)
                .senderId(senderId)
                .receiverId(receiverId)
                .about("Subject")
                .content("Body")
                .sentAt(sentAt)
                .read(true)
                .readAt(sentAt.plusHours(1))
                .build();

        MessageResponse response = MessageMapper.toMessageResponse(message);

        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getSenderId()).isEqualTo(senderId);
        assertThat(response.getReceiverId()).isEqualTo(receiverId);
        assertThat(response.getAbout()).isEqualTo("Subject");
        assertThat(response.getContent()).isEqualTo("Body");
        assertThat(response.isRead()).isTrue();
        assertThat(response.getSentAt()).isEqualTo(sentAt);
        assertThat(response.getReadAt()).isEqualTo(sentAt.plusHours(1));
    }
}
