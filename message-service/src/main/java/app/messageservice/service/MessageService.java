package app.messageservice.service;

import app.messageservice.exception.MessageAccessDeniedException;
import app.messageservice.exception.MessageNotFoundException;
import app.messageservice.mapper.MessageMapper;
import app.messageservice.model.dto.MessageResponse;
import app.messageservice.model.dto.SendMessageRequest;
import app.messageservice.model.entity.Message;
import app.messageservice.repository.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageService.class);

    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Transactional
    public MessageResponse sendMessage(SendMessageRequest request) {
        if (request.getSenderId().equals(request.getReceiverId())) {
            throw new MessageAccessDeniedException();
        }

        Message message = Message.builder()
                .senderId(request.getSenderId())
                .receiverId(request.getReceiverId())
                .content(request.getContent().trim())
                .sentAt(LocalDateTime.now())
                .read(false)
                .build();

        Message saved = messageRepository.save(message);
        log.info("Message {} sent from {} to {}", saved.getId(), saved.getSenderId(), saved.getReceiverId());
        return MessageMapper.toMessageResponse(saved);
    }

    public List<MessageResponse> getInbox(UUID userId) {
        return messageRepository.findByReceiverIdOrderBySentAtDesc(userId)
                .stream()
                .map(MessageMapper::toMessageResponse)
                .toList();
    }

    public long getUnreadCount(UUID userId) {
        return messageRepository.countByReceiverIdAndReadFalse(userId);
    }

    @Transactional
    public MessageResponse markAsRead(UUID messageId, UUID userId) {
        Message message = findMessage(messageId);
        assertReceiver(message, userId);

        if (!message.isRead()) {
            message.setRead(true);
            message.setReadAt(LocalDateTime.now());
            messageRepository.save(message);
            log.info("Message {} marked as read by {}", messageId, userId);
        }

        return MessageMapper.toMessageResponse(message);
    }

    @Transactional
    public void deleteMessage(UUID messageId, UUID userId) {
        Message message = findMessage(messageId);
        if (!message.getReceiverId().equals(userId) && !message.getSenderId().equals(userId)) {
            throw new MessageAccessDeniedException();
        }

        messageRepository.delete(message);
        log.info("Message {} deleted by {}", messageId, userId);
    }

    private Message findMessage(UUID messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(MessageNotFoundException::new);
    }

    private void assertReceiver(Message message, UUID userId) {
        if (!message.getReceiverId().equals(userId)) {
            throw new MessageAccessDeniedException();
        }
    }
}
