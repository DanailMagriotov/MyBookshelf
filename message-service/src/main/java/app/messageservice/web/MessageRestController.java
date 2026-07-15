package app.messageservice.web;

import app.messageservice.model.dto.MessageResponse;
import app.messageservice.model.dto.SendMessageRequest;
import app.messageservice.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/messages")
public class MessageRestController {

    private final MessageService messageService;

    public MessageRestController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse sendMessage(@Valid @RequestBody SendMessageRequest request) {
        return messageService.sendMessage(request);
    }

    @GetMapping("/user/{userId}/inbox")
    public List<MessageResponse> getInbox(@PathVariable UUID userId) {
        return messageService.getInbox(userId);
    }

    @GetMapping("/user/{userId}/unread-count")
    public long getUnreadCount(@PathVariable UUID userId) {
        return messageService.getUnreadCount(userId);
    }

    @PutMapping("/{messageId}/read")
    public MessageResponse markAsRead(@PathVariable UUID messageId,
                                      @RequestParam UUID userId) {
        return messageService.markAsRead(messageId, userId);
    }

    @DeleteMapping("/{messageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMessage(@PathVariable UUID messageId,
                              @RequestParam UUID userId) {
        messageService.deleteMessage(messageId, userId);
    }
}
