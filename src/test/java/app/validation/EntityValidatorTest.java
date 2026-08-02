package app.validation;

import app.exception.EntityValidationException;
import app.model.entity.book.Book;
import app.model.entity.book.Category;
import app.model.entity.user.Region;
import app.model.entity.user.User;
import app.model.entity.user.UserRole;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    void validate_acceptsValidBook() {
        Book book = Book.builder()
                .title("Dune")
                .author("Herbert")
                .category(Category.FANTASY)
                .ownerLabel("my book")
                .owner(validUser())
                .build();

        assertThatCode(() -> entityValidator.validate(book)).doesNotThrowAnyException();
    }

    @Test
    void validate_rejectsBookWithBlankTitle() {
        Book book = Book.builder()
                .title(" ")
                .author("Herbert")
                .category(Category.FANTASY)
                .ownerLabel("my book")
                .owner(validUser())
                .build();

        assertThatThrownBy(() -> entityValidator.validate(book))
                .isInstanceOf(EntityValidationException.class);
    }

    private static User validUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .username("alice")
                .password("encoded")
                .email("alice@example.com")
                .role(UserRole.USER)
                .region(Region.SOFIA)
                .build();
    }
}
