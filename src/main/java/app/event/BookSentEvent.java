package app.event;

import java.util.UUID;

public record BookSentEvent(UUID senderId, UUID receiverId, UUID bookId, String bookTitle) {
}
