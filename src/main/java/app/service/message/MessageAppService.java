package app.service.message;

import app.client.MessageServiceClient;
import app.exception.MessageServiceUnavailableException;
import app.exception.ReceiverNotFoundException;
import app.exception.SelfMessageException;
import app.model.dto.message.MessageDto;
import app.model.dto.message.MessageViewDto;
import app.model.dto.message.SendMessageApiRequest;
import app.model.dto.message.SendMessageFormRequest;
import app.model.entity.user.User;
import app.model.entity.user.UserRole;
import app.repository.user.UserRepository;
import app.service.user.SystemUserService;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MessageAppService {

    public static final int INBOX_PAGE_SIZE = 6;
    public static final int SENT_PAGE_SIZE = 8;

    private static final Logger log = LoggerFactory.getLogger(MessageAppService.class);

    private static final String SYSTEM_MESSAGE_ABOUT = "Overdue return reminder";
    public static final String ROLE_CHANGE_NOTIFICATION_ABOUT = "Role change notification";

    private final MessageServiceClient messageServiceClient;
    private final UserRepository userRepository;
    private final SystemUserService systemUserService;

    public MessageAppService(MessageServiceClient messageServiceClient,
                             UserRepository userRepository,
                             SystemUserService systemUserService) {
        this.messageServiceClient = messageServiceClient;
        this.userRepository = userRepository;
        this.systemUserService = systemUserService;
    }

    public List<MessageViewDto> getInbox(UUID userId) {
        List<MessageDto> messages = call(() -> messageServiceClient.getInbox(userId));
        Map<UUID, User> senders = loadUsers(messages.stream().map(MessageDto::getSenderId).toList());

        return messages.stream()
                .map(message -> toInboxView(message, userId, senders))
                .toList();
    }

    public Page<MessageViewDto> getInboxPage(UUID userId, Pageable pageable) {
        List<MessageViewDto> inbox = getInbox(userId);
        int pageSize = pageable.getPageSize();
        int pageNumber = pageable.getPageNumber();
        int start = pageNumber * pageSize;

        if (start >= inbox.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, inbox.size());
        }

        int end = Math.min(start + pageSize, inbox.size());
        return new PageImpl<>(inbox.subList(start, end), pageable, inbox.size());
    }

    public List<MessageViewDto> getSent(UUID userId) {
        List<MessageDto> messages = call(() -> messageServiceClient.getSent(userId));
        Map<UUID, User> recipients = loadUsers(messages.stream().map(MessageDto::getReceiverId).toList());

        return messages.stream()
                .map(message -> toSentView(message, userId, recipients))
                .toList();
    }

    public Page<MessageViewDto> getSentPage(UUID userId, Pageable pageable) {
        List<MessageViewDto> sent = getSent(userId);
        int pageSize = pageable.getPageSize();
        int pageNumber = pageable.getPageNumber();
        int start = pageNumber * pageSize;

        if (start >= sent.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, sent.size());
        }

        int end = Math.min(start + pageSize, sent.size());
        return new PageImpl<>(sent.subList(start, end), pageable, sent.size());
    }

    public long getUnreadCount(UUID userId) {
        return call(() -> messageServiceClient.getUnreadCount(userId));
    }

    public long getInboxCount(UUID userId) {
        return call(() -> messageServiceClient.getInbox(userId)).size();
    }

    public void sendMessage(UUID senderId, SendMessageFormRequest request) {
        User receiver = userRepository.findByUsername(request.getReceiverUsername().trim())
                .orElseThrow(ReceiverNotFoundException::new);

        if (senderId.equals(receiver.getId())) {
            throw new SelfMessageException();
        }

        SendMessageApiRequest apiRequest = SendMessageApiRequest.builder()
                .senderId(senderId)
                .receiverId(receiver.getId())
                .about(request.getAbout().trim())
                .content(request.getContent().trim())
                .build();

        call(() -> messageServiceClient.sendMessage(apiRequest));
        log.info("User {} sent a message to {}", senderId, receiver.getUsername());
    }

    public void sendSystemMessage(UUID receiverId, String content) {
        sendSystemMessage(receiverId, SYSTEM_MESSAGE_ABOUT, content);
    }

    public void sendSystemMessage(UUID receiverId, String about, String content) {
        SendMessageApiRequest apiRequest = SendMessageApiRequest.builder()
                .senderId(systemUserService.getSystemUserId())
                .receiverId(receiverId)
                .about(about.trim())
                .content(content.trim())
                .build();

        call(() -> messageServiceClient.sendMessage(apiRequest));
        log.info("System message sent to user {}", receiverId);
    }

    public void sendRoleChangeNotification(UUID receiverId, UserRole newRole) {
        String content = newRole == UserRole.ADMIN
                ? "Congratulations! You are promoted to ADMIN."
                : "Sorry, you are demoted to USER.";
        sendSystemMessage(receiverId, ROLE_CHANGE_NOTIFICATION_ABOUT, content);
    }

    public MessageViewDto viewInboxMessage(UUID userId, UUID messageId) {
        MessageDto message = call(() -> messageServiceClient.getInboxMessage(messageId, userId));
        User sender = userRepository.findById(message.getSenderId()).orElse(null);
        String senderUsername = sender != null ? sender.getUsername() : "unknown";

        return MessageViewDto.builder()
                .id(message.getId())
                .senderUsername(senderUsername)
                .about(message.getAbout())
                .content(message.getContent())
                .sentAt(message.getSentAt())
                .read(message.isRead())
                .deletable(true)
                .build();
    }

    public void deleteMessage(UUID userId, UUID messageId) {
        runVoid(() -> messageServiceClient.deleteMessage(messageId, userId));
        log.info("User {} deleted message {}", userId, messageId);
    }

    private MessageViewDto toInboxView(MessageDto message, UUID viewerId, Map<UUID, User> senders) {
        User sender = senders.get(message.getSenderId());
        String senderUsername = sender != null ? sender.getUsername() : "unknown";

        return MessageViewDto.builder()
                .id(message.getId())
                .senderUsername(senderUsername)
                .about(message.getAbout())
                .content(message.getContent())
                .sentAt(message.getSentAt())
                .read(message.isRead())
                .deletable(message.getReceiverId().equals(viewerId) || message.getSenderId().equals(viewerId))
                .build();
    }

    private MessageViewDto toSentView(MessageDto message, UUID viewerId, Map<UUID, User> recipients) {
        User recipient = recipients.get(message.getReceiverId());
        String recipientUsername = recipient != null ? recipient.getUsername() : "unknown";

        return MessageViewDto.builder()
                .id(message.getId())
                .recipientUsername(recipientUsername)
                .about(message.getAbout())
                .content(message.getContent())
                .sentAt(message.getSentAt())
                .read(message.isRead())
                .deletable(message.getSenderId().equals(viewerId))
                .build();
    }

    private Map<UUID, User> loadUsers(List<UUID> userIds) {
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private <T> T call(FeignSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (FeignException ex) {
            log.error("Message service call failed", ex);
            throw new MessageServiceUnavailableException(ex);
        }
    }

    private void runVoid(FeignRunnable runnable) {
        try {
            runnable.run();
        } catch (FeignException ex) {
            log.error("Message service call failed", ex);
            throw new MessageServiceUnavailableException(ex);
        }
    }

    @FunctionalInterface
    private interface FeignSupplier<T> {
        T get();
    }

    @FunctionalInterface
    private interface FeignRunnable {
        void run();
    }
}
