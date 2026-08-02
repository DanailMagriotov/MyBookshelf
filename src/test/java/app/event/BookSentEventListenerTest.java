package app.event;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;

class BookSentEventListenerTest {

    @Test
    void onBookSent_handlesEventWithoutError() {
        BookSentEventListener listener = new BookSentEventListener();
        BookSentEvent event = new BookSentEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Dune");

        assertThatCode(() -> listener.onBookSent(event)).doesNotThrowAnyException();
    }
}
