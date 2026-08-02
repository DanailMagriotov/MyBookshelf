package app.service.message;

import app.client.MessageServiceClient;
import app.exception.MessageServiceUnavailableException;
import app.exception.ReceiverNotFoundException;
import app.exception.SelfMessageException;
import app.model.dto.message.MessageDto;
import app.model.dto.message.SendMessageFormRequest;
import app.model.entity.user.Region;
import app.model.entity.user.User;
import app.model.entity.user.UserRole;
import app.repository.user.UserRepository;
import app.service.user.SystemUserService;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageAppServiceTest {

    @Mock
    private MessageServiceClient messageServiceClient;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SystemUserService systemUserService;

    @InjectMocks
    private MessageAppService messageAppService;

    @Test
    void sendMessage_sendsThroughClient() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        User receiver = user(receiverId, "receiver");

        when(userRepository.findByUsername("receiver")).thenReturn(Optional.of(receiver));

        SendMessageFormRequest request = SendMessageFormRequest.builder()
                .receiverUsername("receiver")
                .about("Hello")
                .content("Content")
                .build();

        messageAppService.sendMessage(senderId, request);

        verify(messageServiceClient).sendMessage(any());
    }

    @Test
    void sendMessage_throwsWhenReceiverMissing() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageAppService.sendMessage(UUID.randomUUID(), SendMessageFormRequest.builder()
                .receiverUsername("missing")
                .about("Hello")
                .content("Content")
                .build()))
                .isInstanceOf(ReceiverNotFoundException.class);
    }

    @Test
    void sendMessage_throwsOnSelfMessage() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user(userId, "alice")));

        assertThatThrownBy(() -> messageAppService.sendMessage(userId, SendMessageFormRequest.builder()
                .receiverUsername("alice")
                .about("Hello")
                .content("Content")
                .build()))
                .isInstanceOf(SelfMessageException.class);
    }

    @Test
    void sendMessage_wrapsFeignErrors() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        when(userRepository.findByUsername("receiver")).thenReturn(Optional.of(user(receiverId, "receiver")));
        when(messageServiceClient.sendMessage(any())).thenThrow(feignException());

        assertThatThrownBy(() -> messageAppService.sendMessage(senderId, SendMessageFormRequest.builder()
                .receiverUsername("receiver")
                .about("Hello")
                .content("Content")
                .build()))
                .isInstanceOf(MessageServiceUnavailableException.class);
    }

    @Test
    void sendSystemMessage_usesSystemUser() {
        UUID receiverId = UUID.randomUUID();
        UUID systemUserId = UUID.randomUUID();
        when(systemUserService.getSystemUserId()).thenReturn(systemUserId);

        messageAppService.sendSystemMessage(receiverId, "Reminder");

        verify(messageServiceClient).sendMessage(any());
        verify(systemUserService).getSystemUserId();
    }

    @Test
    void getInbox_mapsSenderUsernames() {
        UUID viewerId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        MessageDto message = messageDto(UUID.randomUUID(), senderId, viewerId);

        when(messageServiceClient.getInbox(viewerId)).thenReturn(List.of(message));
        when(userRepository.findAllById(List.of(senderId))).thenReturn(List.of(user(senderId, "sender")));

        var inbox = messageAppService.getInbox(viewerId);

        assertThat(inbox).hasSize(1);
        assertThat(inbox.get(0).getSenderUsername()).isEqualTo("sender");
        assertThat(inbox.get(0).isRead()).isFalse();
        assertThat(inbox.get(0).isDeletable()).isTrue();
    }

    @Test
    void getInboxPage_returnsSlice() {
        UUID viewerId = UUID.randomUUID();
        MessageDto message = messageDto(UUID.randomUUID(), UUID.randomUUID(), viewerId);

        when(messageServiceClient.getInbox(viewerId)).thenReturn(List.of(message));
        when(userRepository.findAllById(any())).thenReturn(Collections.emptyList());

        var page = messageAppService.getInboxPage(viewerId, PageRequest.of(0, 1));

        assertThat(page.getTotalElements()).isOne();
        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void getUnreadCount_delegatesToClient() {
        UUID userId = UUID.randomUUID();
        when(messageServiceClient.getUnreadCount(userId)).thenReturn(5L);

        assertThat(messageAppService.getUnreadCount(userId)).isEqualTo(5L);
    }

    @Test
    void getInboxCount_returnsInboxSize() {
        UUID userId = UUID.randomUUID();
        when(messageServiceClient.getInbox(userId)).thenReturn(List.of(messageDto(UUID.randomUUID(), UUID.randomUUID(), userId)));

        assertThat(messageAppService.getInboxCount(userId)).isOne();
    }

    @Test
    void getSent_mapsRecipientUsernames() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        MessageDto message = messageDto(UUID.randomUUID(), senderId, receiverId);

        when(messageServiceClient.getSent(senderId)).thenReturn(List.of(message));
        when(userRepository.findAllById(List.of(receiverId))).thenReturn(List.of(user(receiverId, "receiver")));

        var sent = messageAppService.getSent(senderId);

        assertThat(sent).hasSize(1);
        assertThat(sent.get(0).getRecipientUsername()).isEqualTo("receiver");
    }

    @Test
    void getSentPage_returnsSlice() {
        UUID senderId = UUID.randomUUID();
        MessageDto message = messageDto(UUID.randomUUID(), senderId, UUID.randomUUID());

        when(messageServiceClient.getSent(senderId)).thenReturn(List.of(message));
        when(userRepository.findAllById(any())).thenReturn(Collections.emptyList());

        var page = messageAppService.getSentPage(senderId, PageRequest.of(0, 1));

        assertThat(page.getTotalElements()).isOne();
    }

    @Test
    void deleteMessage_delegatesToClient() {
        UUID userId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        messageAppService.deleteMessage(userId, messageId);

        verify(messageServiceClient).deleteMessage(messageId, userId);
    }

    @Test
    void viewInboxMessage_returnsMappedMessage() {
        UUID userId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        MessageDto message = messageDto(messageId, senderId, userId);

        when(messageServiceClient.getInboxMessage(messageId, userId)).thenReturn(message);
        when(userRepository.findById(senderId)).thenReturn(Optional.of(user(senderId, "sender")));

        var view = messageAppService.viewInboxMessage(userId, messageId);

        assertThat(view.getSenderUsername()).isEqualTo("sender");
        assertThat(view.getAbout()).isEqualTo("Subject");
    }

    private static User user(UUID id, String username) {
        return User.builder()
                .id(id)
                .username(username)
                .password("secret")
                .email(username + "@example.com")
                .role(UserRole.USER)
                .region(Region.SOFIA)
                .build();
    }

    private static MessageDto messageDto(UUID id, UUID senderId, UUID receiverId) {
        return MessageDto.builder()
                .id(id)
                .senderId(senderId)
                .receiverId(receiverId)
                .about("Subject")
                .content("Body")
                .sentAt(LocalDateTime.now())
                .read(false)
                .build();
    }

    private static FeignException feignException() {
        Request request = Request.create(
                Request.HttpMethod.POST,
                "http://localhost:8081/api/messages",
                Map.of(),
                null,
                StandardCharsets.UTF_8,
                new RequestTemplate()
        );
        return FeignException.errorStatus("sendMessage", feign.Response.builder()
                .status(503)
                .reason("Unavailable")
                .request(request)
                .headers(Map.of())
                .build());
    }
}
