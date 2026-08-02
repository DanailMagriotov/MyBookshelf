package app.messageservice.service;

import app.messageservice.exception.MessageAccessDeniedException;
import app.messageservice.exception.MessageNotFoundException;
import app.messageservice.mapper.MessageMapper;
import app.messageservice.model.dto.MessageResponse;
import app.messageservice.model.dto.SendMessageRequest;
import app.messageservice.model.entity.Message;
import app.messageservice.repository.MessageRepository;
import app.messageservice.validation.EntityValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageService.class);

    private final MessageRepository messageRepository;
    private final EntityValidator entityValidator;

    public MessageService(MessageRepository messageRepository, EntityValidator entityValidator) {
        this.messageRepository = messageRepository;
        this.entityValidator = entityValidator;
    }

    @Transactional
    @CacheEvict(value = "unreadCounts", allEntries = true)
    public MessageResponse sendMessage(SendMessageRequest request) {
        if (request.getSenderId().equals(request.getReceiverId())) {
            throw new MessageAccessDeniedException();
        }

        Message message = Message.builder()
                .senderId(request.getSenderId())
                .receiverId(request.getReceiverId())
                .about(request.getAbout().trim())
                .content(request.getContent().trim())
                .sentAt(LocalDateTime.now())
                .read(false)
                .hiddenFromSender(false)
                .hiddenFromReceiver(false)
                .build();

        entityValidator.validate(message);
        Message saved = messageRepository.save(message);
        log.info("Message {} sent from {} to {}", saved.getId(), saved.getSenderId(), saved.getReceiverId());
        return MessageMapper.toMessageResponse(saved);
    }

    public List<MessageResponse> getInbox(UUID userId) {
        return messageRepository.findByReceiverIdAndHiddenFromReceiverFalseOrderBySentAtDesc(userId)
                .stream()
                .map(MessageMapper::toMessageResponse)
                .toList();
    }

    public List<MessageResponse> getSent(UUID userId) {
        return messageRepository.findBySenderIdAndHiddenFromSenderFalseOrderBySentAtDesc(userId)
                .stream()
                .map(MessageMapper::toMessageResponse)
                .toList();
    }

    @Cacheable(value = "unreadCounts", key = "#userId")
    public long getUnreadCount(UUID userId) {
        return messageRepository.countByReceiverIdAndHiddenFromReceiverFalseAndReadFalse(userId);
    }

    @Transactional
    @CacheEvict(value = "unreadCounts", allEntries = true)
    public MessageResponse getInboxMessage(UUID messageId, UUID userId) {
        Message message = findMessage(messageId);
        assertReceiver(message, userId);

        if (!message.isRead()) {
            message.setRead(true);
            message.setReadAt(LocalDateTime.now());
            entityValidator.validate(message);
            messageRepository.save(message);
            log.info("Message {} marked as read by {}", messageId, userId);
        }

        return MessageMapper.toMessageResponse(message);
    }

    @Transactional
    @CacheEvict(value = "unreadCounts", allEntries = true)
    public void deleteMessage(UUID messageId, UUID userId) {
        Message message = findMessage(messageId);

        if (message.getSenderId().equals(userId)) {
            message.setHiddenFromSender(true);
            persistOrDelete(message);
            log.info("Message {} hidden from sent list for {}", messageId, userId);
            return;
        }

        if (message.getReceiverId().equals(userId)) {
            message.setHiddenFromReceiver(true);
            persistOrDelete(message);
            log.info("Message {} hidden from inbox for {}", messageId, userId);
            return;
        }

        throw new MessageAccessDeniedException();
    }

    private void persistOrDelete(Message message) {
        if (message.isHiddenFromSender() && message.isHiddenFromReceiver()) {
            messageRepository.delete(message);
            log.info("Message {} permanently deleted from database", message.getId());
            return;
        }

        entityValidator.validate(message);
        messageRepository.save(message);
    }

    private Message findMessage(UUID messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(MessageNotFoundException::new);
    }

    private void assertReceiver(Message message, UUID userId) {
        if (!message.getReceiverId().equals(userId)) {
            throw new MessageAccessDeniedException();
        }
        if (message.isHiddenFromReceiver()) {
            throw new MessageNotFoundException();
        }
    }
}
