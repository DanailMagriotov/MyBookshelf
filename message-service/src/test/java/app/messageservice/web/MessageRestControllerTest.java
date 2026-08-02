package app.messageservice.web;

import app.messageservice.model.dto.MessageResponse;
import app.messageservice.model.dto.SendMessageRequest;
import app.messageservice.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MessageRestControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private MessageRestController messageRestController;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(messageRestController)
                .setValidator(validator)
                .setControllerAdvice(new MessageExceptionHandler())
                .build();
    }

    @Test
    void sendMessage_returnsCreated() throws Exception {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        MessageResponse response = MessageResponse.builder()
                .id(messageId)
                .senderId(senderId)
                .receiverId(receiverId)
                .about("Hello")
                .content("Body")
                .sentAt(LocalDateTime.now())
                .read(false)
                .build();

        when(messageService.sendMessage(any(SendMessageRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "senderId": "%s",
                                  "receiverId": "%s",
                                  "about": "Hello",
                                  "content": "Body"
                                }
                                """.formatted(senderId, receiverId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(messageId.toString()));
    }

    @Test
    void getInbox_returnsMessages() throws Exception {
        UUID userId = UUID.randomUUID();
        when(messageService.getInbox(userId)).thenReturn(List.of());

        mockMvc.perform(get("/api/messages/user/{userId}/inbox", userId))
                .andExpect(status().isOk());
    }

    @Test
    void getSent_returnsMessages() throws Exception {
        UUID userId = UUID.randomUUID();
        when(messageService.getSent(userId)).thenReturn(List.of());

        mockMvc.perform(get("/api/messages/user/{userId}/sent", userId))
                .andExpect(status().isOk());
    }

    @Test
    void getUnreadCount_returnsCount() throws Exception {
        UUID userId = UUID.randomUUID();
        when(messageService.getUnreadCount(userId)).thenReturn(3L);

        mockMvc.perform(get("/api/messages/user/{userId}/unread-count", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(3));
    }

    @Test
    void getInboxMessage_delegatesToService() throws Exception {
        UUID messageId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(messageService.getInboxMessage(messageId, userId)).thenReturn(MessageResponse.builder()
                .id(messageId)
                .read(true)
                .build());

        mockMvc.perform(get("/api/messages/{messageId}/inbox", messageId)
                        .param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(messageId.toString()));

        verify(messageService).getInboxMessage(messageId, userId);
    }

    @Test
    void sendMessage_withInvalidBody_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "senderId": "%s",
                                  "receiverId": "%s",
                                  "about": "",
                                  "content": ""
                                }
                                """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void deleteMessage_returnsNoContent() throws Exception {
        UUID messageId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        mockMvc.perform(delete("/api/messages/{messageId}", messageId)
                        .param("userId", userId.toString()))
                .andExpect(status().isNoContent());

        verify(messageService).deleteMessage(messageId, userId);
    }
}
