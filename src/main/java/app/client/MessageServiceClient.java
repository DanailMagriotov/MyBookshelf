package app.client;

import app.model.dto.message.MessageDto;
import app.model.dto.message.SendMessageApiRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "message-service", url = "${message-service.url}")
public interface MessageServiceClient {

    @PostMapping("/api/messages")
    MessageDto sendMessage(@RequestBody SendMessageApiRequest request);

    @GetMapping("/api/messages/user/{userId}/inbox")
    List<MessageDto> getInbox(@PathVariable("userId") UUID userId);

    @GetMapping("/api/messages/user/{userId}/unread-count")
    long getUnreadCount(@PathVariable("userId") UUID userId);

    @PutMapping("/api/messages/{messageId}/read")
    MessageDto markAsRead(@PathVariable("messageId") UUID messageId,
                          @RequestParam("userId") UUID userId);

    @DeleteMapping("/api/messages/{messageId}")
    void deleteMessage(@PathVariable("messageId") UUID messageId,
                       @RequestParam("userId") UUID userId);
}
