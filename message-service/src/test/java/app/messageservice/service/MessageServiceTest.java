package app.messageservice.service;

import app.messageservice.exception.MessageAccessDeniedException;
import app.messageservice.exception.MessageNotFoundException;
import app.messageservice.model.dto.SendMessageRequest;
import app.messageservice.model.entity.Message;
import app.messageservice.repository.MessageRepository;
import app.messageservice.validation.EntityValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private EntityValidator entityValidator;

    @InjectMocks
    private MessageService messageService;

    @Test
    void sendMessage_savesMessage() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        SendMessageRequest request = SendMessageRequest.builder()
                .senderId(senderId)
                .receiverId(receiverId)
                .about(" Subject ")
                .content(" Content ")
                .build();
        Message saved = message(UUID.randomUUID(), senderId, receiverId);

        when(messageRepository.save(any(Message.class))).thenReturn(saved);

        var response = messageService.sendMessage(request);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(entityValidator).validate(any(Message.class));
        verify(messageRepository).save(captor.capture());
        assertThat(captor.getValue().getAbout()).isEqualTo("Subject");
        assertThat(captor.getValue().getContent()).isEqualTo("Content");
        assertThat(response.getSenderId()).isEqualTo(senderId);
    }

    @Test
    void sendMessage_throwsOnSelfMessage() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> messageService.sendMessage(SendMessageRequest.builder()
                .senderId(userId)
                .receiverId(userId)
                .about("Subject")
                .content("Content")
                .build()))
                .isInstanceOf(MessageAccessDeniedException.class);
    }

    @Test
    void getInbox_returnsVisibleMessages() {
        UUID userId = UUID.randomUUID();
        Message message = message(UUID.randomUUID(), UUID.randomUUID(), userId);
        when(messageRepository.findByReceiverIdAndHiddenFromReceiverFalseOrderBySentAtDesc(userId))
                .thenReturn(List.of(message));

        var inbox = messageService.getInbox(userId);

        assertThat(inbox).hasSize(1);
        assertThat(inbox.get(0).getReceiverId()).isEqualTo(userId);
    }

    @Test
    void getSent_returnsVisibleMessages() {
        UUID userId = UUID.randomUUID();
        Message message = message(UUID.randomUUID(), userId, UUID.randomUUID());
        when(messageRepository.findBySenderIdAndHiddenFromSenderFalseOrderBySentAtDesc(userId))
                .thenReturn(List.of(message));

        var sent = messageService.getSent(userId);

        assertThat(sent).hasSize(1);
        assertThat(sent.get(0).getSenderId()).isEqualTo(userId);
    }

    @Test
    void getUnreadCount_delegatesToRepository() {
        UUID userId = UUID.randomUUID();
        when(messageRepository.countByReceiverIdAndHiddenFromReceiverFalseAndReadFalse(userId)).thenReturn(2L);

        assertThat(messageService.getUnreadCount(userId)).isEqualTo(2L);
    }

    @Test
    void markAsRead_marksUnreadMessageAsRead() {
        UUID userId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        Message message = message(messageId, UUID.randomUUID(), userId);
        message.setRead(false);

        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));

        var response = messageService.markAsRead(messageId, userId);

        assertThat(response.isRead()).isTrue();
        verify(messageRepository).save(message);
    }

    @Test
    void getInboxMessage_marksUnreadMessageAsRead() {
        UUID userId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        Message message = message(messageId, UUID.randomUUID(), userId);
        message.setRead(false);

        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));

        var response = messageService.getInboxMessage(messageId, userId);

        assertThat(response.isRead()).isTrue();
        verify(messageRepository).save(message);
    }

    @Test
    void getInboxMessage_throwsWhenHiddenFromReceiver() {
        UUID userId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        Message message = message(messageId, UUID.randomUUID(), userId);
        message.setHiddenFromReceiver(true);

        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));

        assertThatThrownBy(() -> messageService.getInboxMessage(messageId, userId))
                .isInstanceOf(MessageNotFoundException.class);
    }

    @Test
    void deleteMessage_hidesFromSenderAndDeletesWhenBothHidden() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        Message message = message(messageId, senderId, receiverId);
        message.setHiddenFromReceiver(true);

        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));

        messageService.deleteMessage(messageId, senderId);

        assertThat(message.isHiddenFromSender()).isTrue();
        verify(messageRepository).delete(message);
    }

    @Test
    void deleteMessage_hidesFromReceiverOnly() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        Message message = message(messageId, senderId, receiverId);

        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));

        messageService.deleteMessage(messageId, receiverId);

        assertThat(message.isHiddenFromReceiver()).isTrue();
        verify(messageRepository).save(message);
        verify(messageRepository, never()).delete(message);
    }

    @Test
    void deleteMessage_throwsForOtherUser() {
        UUID messageId = UUID.randomUUID();
        Message message = message(messageId, UUID.randomUUID(), UUID.randomUUID());

        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));

        assertThatThrownBy(() -> messageService.deleteMessage(messageId, UUID.randomUUID()))
                .isInstanceOf(MessageAccessDeniedException.class);
    }

    @Test
    void markAsRead_doesNotSaveWhenAlreadyRead() {
        UUID userId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        Message message = message(messageId, UUID.randomUUID(), userId);
        message.setRead(true);

        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));

        messageService.markAsRead(messageId, userId);

        verify(messageRepository, never()).save(message);
    }

    @Test
    void deleteMessage_throwsWhenMessageMissing() {
        UUID messageId = UUID.randomUUID();
        when(messageRepository.findById(messageId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.deleteMessage(messageId, UUID.randomUUID()))
                .isInstanceOf(MessageNotFoundException.class);
    }

    @Test
    void getInboxMessage_throwsWhenUserIsNotReceiver() {
        UUID messageId = UUID.randomUUID();
        Message message = message(messageId, UUID.randomUUID(), UUID.randomUUID());

        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));

        assertThatThrownBy(() -> messageService.getInboxMessage(messageId, UUID.randomUUID()))
                .isInstanceOf(MessageAccessDeniedException.class);
    }

    @Test
    void getInboxMessage_doesNotSaveWhenAlreadyRead() {
        UUID userId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        Message message = message(messageId, UUID.randomUUID(), userId);
        message.setRead(true);
        message.setReadAt(LocalDateTime.now());

        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));

        var response = messageService.getInboxMessage(messageId, userId);

        assertThat(response.isRead()).isTrue();
        verify(messageRepository, never()).save(message);
    }

    @Test
    void markAsRead_throwsWhenMessageMissing() {
        UUID messageId = UUID.randomUUID();
        when(messageRepository.findById(messageId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.markAsRead(messageId, UUID.randomUUID()))
                .isInstanceOf(MessageNotFoundException.class);
    }

    @Test
    void markAsRead_throwsWhenUserIsNotReceiver() {
        UUID messageId = UUID.randomUUID();
        Message message = message(messageId, UUID.randomUUID(), UUID.randomUUID());

        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));

        assertThatThrownBy(() -> messageService.markAsRead(messageId, UUID.randomUUID()))
                .isInstanceOf(MessageAccessDeniedException.class);
    }

    @Test
    void deleteMessage_hidesFromSenderOnly() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        Message message = message(messageId, senderId, receiverId);

        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));

        messageService.deleteMessage(messageId, senderId);

        assertThat(message.isHiddenFromSender()).isTrue();
        verify(messageRepository).save(message);
        verify(messageRepository, never()).delete(message);
    }

    private static Message message(UUID id, UUID senderId, UUID receiverId) {
        return Message.builder()
                .id(id)
                .senderId(senderId)
                .receiverId(receiverId)
                .about("Subject")
                .content("Content")
                .sentAt(LocalDateTime.now())
                .read(false)
                .hiddenFromSender(false)
                .hiddenFromReceiver(false)
                .build();
    }
}
