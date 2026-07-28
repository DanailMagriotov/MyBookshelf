package app.messageservice.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExceptionClassesTest {

    @Test
    void messageNotFoundException_canBeThrown() {
        assertThatThrownBy(() -> {
            throw new MessageNotFoundException();
        }).isInstanceOf(MessageNotFoundException.class)
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void messageAccessDeniedException_canBeThrown() {
        assertThatThrownBy(() -> {
            throw new MessageAccessDeniedException();
        }).isInstanceOf(MessageAccessDeniedException.class)
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void exceptions_haveDistinctTypes() {
        assertThat(new MessageNotFoundException())
                .isNotExactlyInstanceOf(MessageAccessDeniedException.class);
        assertThat(new MessageAccessDeniedException())
                .isNotExactlyInstanceOf(MessageNotFoundException.class);
    }
}
