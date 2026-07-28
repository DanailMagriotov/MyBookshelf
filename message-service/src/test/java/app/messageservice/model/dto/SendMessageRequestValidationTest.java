package app.messageservice.model.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SendMessageRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDownValidator() {
        validatorFactory.close();
    }

    @Test
    void validRequest_hasNoViolations() {
        SendMessageRequest request = SendMessageRequest.builder()
                .senderId(UUID.randomUUID())
                .receiverId(UUID.randomUUID())
                .about("Subject")
                .content("Body")
                .build();

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void invalidRequest_reportsFieldErrors() {
        SendMessageRequest request = SendMessageRequest.builder().build();

        Set<?> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void apiErrorResponse_builderWorks() {
        ApiErrorResponse response = ApiErrorResponse.builder()
                .message("Error")
                .build();

        assertThat(response.getMessage()).isEqualTo("Error");
    }

    @Test
    void messageResponse_builderWorks() {
        MessageResponse response = MessageResponse.builder()
                .id(UUID.randomUUID())
                .senderId(UUID.randomUUID())
                .receiverId(UUID.randomUUID())
                .about("Hi")
                .content("Body")
                .read(false)
                .build();

        assertThat(response.getAbout()).isEqualTo("Hi");
        assertThat(response.isRead()).isFalse();
    }
}
