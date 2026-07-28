package app.web;

import app.exception.MessageServiceUnavailableException;
import app.exception.ReceiverNotFoundException;
import app.exception.SelfMessageException;
import app.model.dto.message.MessageViewDto;
import app.service.message.MessageAppService;
import app.service.user.UserService;
import app.service.user.UserSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static app.model.entity.user.UserRole.USER;
import static app.testsupport.WebTestSupport.sessionWith;
import static app.testsupport.WebTestSupport.standaloneWithPageable;
import static app.testsupport.WebTestSupport.userSession;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class MessagesControllerApiTest {

    @Mock
    private MessageAppService messageAppService;

    @Mock
    private UserService userService;

    @Mock
    private UserSessionService userSessionService;

    private MockMvc mockMvc;

    private final UUID userId = UUID.randomUUID();
    private final UUID messageId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = standaloneWithPageable(
                new MessagesController(messageAppService, userService, userSessionService)).build();
    }

    private app.model.dto.user.UserSession authenticatedSession() {
        return userSession(userId, USER);
    }

    @Test
    void messagesHub_returnsHubView() throws Exception {
        var session = authenticatedSession();
        when(userSessionService.get(any())).thenReturn(Optional.of(session));
        when(messageAppService.getUnreadCount(userId)).thenReturn(2L);
        when(messageAppService.getInboxCount(userId)).thenReturn(5L);

        mockMvc.perform(get("/messages").session(sessionWith(session)))
                .andExpect(status().isOk())
                .andExpect(view().name("messages-hub"));
    }

    @Test
    void messagesHub_whenServiceUnavailable_returnsHubView() throws Exception {
        var session = authenticatedSession();
        when(userSessionService.get(any())).thenReturn(Optional.of(session));
        when(messageAppService.getUnreadCount(userId)).thenThrow(new MessageServiceUnavailableException(new RuntimeException("unavailable")));

        mockMvc.perform(get("/messages").session(sessionWith(session)))
                .andExpect(status().isOk())
                .andExpect(view().name("messages-hub"));
    }

    @Test
    void newMessagePage_returnsNewMessageView() throws Exception {
        var session = authenticatedSession();
        when(userSessionService.get(any())).thenReturn(Optional.of(session));

        mockMvc.perform(get("/messages/new").session(sessionWith(session)))
                .andExpect(status().isOk())
                .andExpect(view().name("messages-new"));
    }

    @Test
    void inboxPage_returnsInboxView() throws Exception {
        var session = authenticatedSession();
        var message = MessageViewDto.builder()
                .id(messageId)
                .about("Hello")
                .content("Hi")
                .sentAt(LocalDateTime.now())
                .build();
        var page = new PageImpl<>(List.of(message), PageRequest.of(0, MessageAppService.INBOX_PAGE_SIZE), 1);

        when(userSessionService.get(any())).thenReturn(Optional.of(session));
        when(messageAppService.getInboxPage(eq(userId), any())).thenReturn(page);
        when(messageAppService.getUnreadCount(userId)).thenReturn(1L);

        mockMvc.perform(get("/messages/inbox").session(sessionWith(session)))
                .andExpect(status().isOk())
                .andExpect(view().name("messages-inbox"));
    }

    @Test
    void inboxPage_whenServiceUnavailable_returnsInboxView() throws Exception {
        var session = authenticatedSession();
        when(userSessionService.get(any())).thenReturn(Optional.of(session));
        when(messageAppService.getInboxPage(eq(userId), any()))
                .thenThrow(new MessageServiceUnavailableException(new RuntimeException("unavailable")));

        mockMvc.perform(get("/messages/inbox").session(sessionWith(session)))
                .andExpect(status().isOk())
                .andExpect(view().name("messages-inbox"));
    }

    @Test
    void viewInboxMessage_returnsMessageView() throws Exception {
        var session = authenticatedSession();
        var message = MessageViewDto.builder().id(messageId).about("Hello").content("Hi").build();

        when(userSessionService.get(any())).thenReturn(Optional.of(session));
        when(messageAppService.viewInboxMessage(userId, messageId)).thenReturn(message);

        mockMvc.perform(get("/messages/inbox/{messageId}", messageId).session(sessionWith(session)))
                .andExpect(status().isOk())
                .andExpect(view().name("messages-inbox-view"));
    }

    @Test
    void viewInboxMessage_whenUnavailable_returnsViewWithError() throws Exception {
        var session = authenticatedSession();
        when(userSessionService.get(any())).thenReturn(Optional.of(session));
        when(messageAppService.viewInboxMessage(userId, messageId))
                .thenThrow(new MessageServiceUnavailableException(new RuntimeException("unavailable")));

        mockMvc.perform(get("/messages/inbox/{messageId}", messageId).session(sessionWith(session)))
                .andExpect(status().isOk())
                .andExpect(view().name("messages-inbox-view"));
    }

    @Test
    void sentPage_returnsSentView() throws Exception {
        var session = authenticatedSession();
        var page = new PageImpl<>(List.<MessageViewDto>of(), PageRequest.of(0, MessageAppService.SENT_PAGE_SIZE), 0);

        when(userSessionService.get(any())).thenReturn(Optional.of(session));
        when(messageAppService.getSentPage(eq(userId), any())).thenReturn(page);

        mockMvc.perform(get("/messages/sent").session(sessionWith(session)))
                .andExpect(status().isOk())
                .andExpect(view().name("messages-sent"));
    }

    @Test
    void sendMessage_withValidData_redirectsToNewMessage() throws Exception {
        var session = authenticatedSession();
        when(userSessionService.get(any())).thenReturn(Optional.of(session));
        when(userService.isUnknownUsername("bob")).thenReturn(false);
        doNothing().when(messageAppService).sendMessage(eq(userId), any());

        mockMvc.perform(post("/messages/send")
                        .session(sessionWith(session))
                        .param("receiverUsername", "bob")
                        .param("about", "Hello")
                        .param("content", "How are you?"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/messages/new"))
                .andExpect(flash().attribute("successMessage", "Message sent successfully."));
    }

    @Test
    void sendMessage_withUnknownReceiver_returnsNewMessageView() throws Exception {
        var session = authenticatedSession();
        when(userSessionService.get(any())).thenReturn(Optional.of(session));
        when(userService.isUnknownUsername("unknown")).thenReturn(true);

        mockMvc.perform(post("/messages/send")
                        .session(sessionWith(session))
                        .param("receiverUsername", "unknown")
                        .param("about", "Hello")
                        .param("content", "Hi"))
                .andExpect(status().isOk())
                .andExpect(view().name("messages-new"));
    }

    @Test
    void sendMessage_toSelf_returnsNewMessageView() throws Exception {
        var session = authenticatedSession();
        when(userSessionService.get(any())).thenReturn(Optional.of(session));
        when(userService.isUnknownUsername("alice")).thenReturn(false);
        doThrow(new SelfMessageException()).when(messageAppService).sendMessage(eq(userId), any());

        mockMvc.perform(post("/messages/send")
                        .session(sessionWith(session))
                        .param("receiverUsername", "alice")
                        .param("about", "Hello")
                        .param("content", "Hi"))
                .andExpect(status().isOk())
                .andExpect(view().name("messages-new"));
    }

    @Test
    void sendMessage_whenServiceUnavailable_returnsNewMessageView() throws Exception {
        var session = authenticatedSession();
        when(userSessionService.get(any())).thenReturn(Optional.of(session));
        when(userService.isUnknownUsername("bob")).thenReturn(false);
        doThrow(new MessageServiceUnavailableException(new RuntimeException("unavailable"))).when(messageAppService).sendMessage(eq(userId), any());

        mockMvc.perform(post("/messages/send")
                        .session(sessionWith(session))
                        .param("receiverUsername", "bob")
                        .param("about", "Hello")
                        .param("content", "Hi"))
                .andExpect(status().isOk())
                .andExpect(view().name("messages-new"));
    }

    @Test
    void sendMessage_withReceiverNotFound_returnsNewMessageView() throws Exception {
        var session = authenticatedSession();
        when(userSessionService.get(any())).thenReturn(Optional.of(session));
        when(userService.isUnknownUsername("bob")).thenReturn(false);
        doThrow(new ReceiverNotFoundException()).when(messageAppService).sendMessage(eq(userId), any());

        mockMvc.perform(post("/messages/send")
                        .session(sessionWith(session))
                        .param("receiverUsername", "bob")
                        .param("about", "Hello")
                        .param("content", "Hi"))
                .andExpect(status().isOk())
                .andExpect(view().name("messages-new"));
    }

    @Test
    void markAsRead_redirectsToInbox() throws Exception {
        var session = authenticatedSession();
        when(userSessionService.get(any())).thenReturn(Optional.of(session));
        doNothing().when(messageAppService).markAsRead(userId, messageId);

        mockMvc.perform(post("/messages/{messageId}/read", messageId).session(sessionWith(session)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/messages/inbox"))
                .andExpect(flash().attribute("successMessage", "Message marked as read."));
    }

    @Test
    void markAsRead_whenUnavailable_redirectsWithServiceError() throws Exception {
        var session = authenticatedSession();
        when(userSessionService.get(any())).thenReturn(Optional.of(session));
        doThrow(new MessageServiceUnavailableException(new RuntimeException("unavailable"))).when(messageAppService).markAsRead(userId, messageId);

        mockMvc.perform(post("/messages/{messageId}/read", messageId).session(sessionWith(session)))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("serviceError",
                        "Messaging service is unavailable. Please try again later."));
    }

    @Test
    void deleteMessage_fromInbox_redirectsToInbox() throws Exception {
        var session = authenticatedSession();
        when(userSessionService.get(any())).thenReturn(Optional.of(session));
        doNothing().when(messageAppService).deleteMessage(userId, messageId);

        mockMvc.perform(post("/messages/{messageId}/delete", messageId)
                        .session(sessionWith(session))
                        .param("page", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/messages/inbox?page=1"));
    }

    @Test
    void deleteMessage_fromSent_redirectsToSent() throws Exception {
        var session = authenticatedSession();
        when(userSessionService.get(any())).thenReturn(Optional.of(session));
        doNothing().when(messageAppService).deleteMessage(userId, messageId);

        mockMvc.perform(post("/messages/{messageId}/delete", messageId)
                        .session(sessionWith(session))
                        .param("returnTo", "sent")
                        .param("page", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/messages/sent?page=2"));

        verify(messageAppService).deleteMessage(userId, messageId);
    }
}
