package app.messageservice;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.mockito.Mockito.mockStatic;

class MessageServiceApplicationTest {

    @Test
    void main_startsSpringApplication() {
        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            MessageServiceApplication.main(new String[]{});

            springApplication.verify(() -> SpringApplication.run(MessageServiceApplication.class, new String[]{}));
        }
    }
}
