package app.messageservice.validation;

import app.messageservice.exception.EntityValidationException;
import app.messageservice.model.entity.Message;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EntityValidatorTest {

    private ValidatorFactory validatorFactory;
    private EntityValidator entityValidator;

    @BeforeEach
    void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        entityValidator = new EntityValidator(validatorFactory.getValidator());
    }

    @AfterEach
    void tearDown() {
        validatorFactory.close();
    }

    @Test
    void validate_acceptsValidMessage() {
        Message message = Message.builder()
                .senderId(UUID.randomUUID())
                .receiverId(UUID.randomUUID())
                .about("Subject")
                .content("Body")
                .sentAt(LocalDateTime.now())
                .build();

        assertThatCode(() -> entityValidator.validate(message)).doesNotThrowAnyException();
    }

    @Test
    void validate_rejectsBlankSubject() {
        Message message = Message.builder()
                .senderId(UUID.randomUUID())
                .receiverId(UUID.randomUUID())
                .about(" ")
                .content("Body")
                .sentAt(LocalDateTime.now())
                .build();

        assertThatThrownBy(() -> entityValidator.validate(message))
                .isInstanceOf(EntityValidationException.class);
    }
}
