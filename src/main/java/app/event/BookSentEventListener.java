package app.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class BookSentEventListener {

    private static final Logger log = LoggerFactory.getLogger(BookSentEventListener.class);

    @EventListener
    public void onBookSent(BookSentEvent event) {
        log.info("BookSentEvent: user {} sent book '{}' ({}) to user {}",
                event.senderId(), event.bookTitle(), event.bookId(), event.receiverId());
    }
}
