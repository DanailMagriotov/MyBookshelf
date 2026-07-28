package app.messageservice.repository;

import app.messageservice.model.entity.Message;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class MessageRepositoryIntegrationTest {

    @Autowired
    private MessageRepository messageRepository;

    @Test
    void repository_persistsAndQueriesMessages() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        Message message = Message.builder()
                .senderId(senderId)
                .receiverId(receiverId)
                .about("Subject")
                .content("Body")
                .sentAt(LocalDateTime.now())
                .read(false)
                .hiddenFromSender(false)
                .hiddenFromReceiver(false)
                .build();

        messageRepository.save(message);

        assertThat(messageRepository.findById(message.getId())).isPresent();
        assertThat(messageRepository.findByReceiverIdAndHiddenFromReceiverFalseOrderBySentAtDesc(receiverId))
                .hasSize(1);
        assertThat(messageRepository.findBySenderIdAndHiddenFromSenderFalseOrderBySentAtDesc(senderId))
                .hasSize(1);
        assertThat(messageRepository.countByReceiverIdAndHiddenFromReceiverFalseAndReadFalse(receiverId))
                .isEqualTo(1);
        assertThat(messageRepository.count()).isEqualTo(1);
    }

    @Test
    void repository_excludesHiddenMessages() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        Message message = Message.builder()
                .senderId(senderId)
                .receiverId(receiverId)
                .about("Subject")
                .content("Body")
                .sentAt(LocalDateTime.now())
                .read(true)
                .hiddenFromSender(true)
                .hiddenFromReceiver(true)
                .build();

        messageRepository.save(message);

        assertThat(messageRepository.findByReceiverIdAndHiddenFromReceiverFalseOrderBySentAtDesc(receiverId))
                .isEmpty();
        assertThat(messageRepository.findBySenderIdAndHiddenFromSenderFalseOrderBySentAtDesc(senderId))
                .isEmpty();
        assertThat(messageRepository.countByReceiverIdAndHiddenFromReceiverFalseAndReadFalse(receiverId))
                .isZero();
    }
}
