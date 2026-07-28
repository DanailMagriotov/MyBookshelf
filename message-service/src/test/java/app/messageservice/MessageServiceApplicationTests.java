package app.messageservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import com.jayway.jsonpath.JsonPath;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class MessageServiceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void inboxEndpointIsAvailable() throws Exception {
        mockMvc.perform(get("/api/messages/user/{userId}/inbox", UUID.randomUUID()))
                .andExpect(status().isOk());
    }

    @Test
    void messageLifecycle_worksEndToEnd() throws Exception {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();

        MvcResult createResult = mockMvc.perform(post("/api/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "senderId": "%s",
                                  "receiverId": "%s",
                                  "about": "Hello",
                                  "content": "Integration test body"
                                }
                                """.formatted(senderId, receiverId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.read").value(false))
                .andExpect(jsonPath("$.about").value("Hello"))
                .andReturn();

        String messageId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/messages/user/{userId}/unread-count", receiverId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(1));

        mockMvc.perform(get("/api/messages/user/{userId}/inbox", receiverId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(messageId));

        mockMvc.perform(get("/api/messages/{messageId}/inbox", messageId)
                        .param("userId", receiverId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));

        mockMvc.perform(get("/api/messages/user/{userId}/sent", senderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(messageId));

        mockMvc.perform(put("/api/messages/{messageId}/read", messageId)
                        .param("userId", receiverId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));

        mockMvc.perform(delete("/api/messages/{messageId}", messageId)
                        .param("userId", receiverId.toString()))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/messages/{messageId}", messageId)
                        .param("userId", senderId.toString()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/messages/user/{userId}/inbox", receiverId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(get("/api/messages/user/{userId}/sent", senderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void sendMessageToSelf_returnsForbidden() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/api/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "senderId": "%s",
                                  "receiverId": "%s",
                                  "about": "Hello",
                                  "content": "Body"
                                }
                                """.formatted(userId, userId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Operation not allowed for this message"));
    }

    @Test
    void sendMessageWithInvalidBody_returnsBadRequest() throws Exception {
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
}
