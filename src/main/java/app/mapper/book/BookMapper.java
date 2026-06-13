package app.mapper.book;

import app.model.dto.book.AddBookRequest;
import app.model.dto.book.MyBookshelfBookDto;
import app.model.entity.book.Book;
import app.model.entity.booktransfer.BookTransfer;
import app.model.entity.user.User;
import lombok.NoArgsConstructor;

import java.util.UUID;

@NoArgsConstructor
public class BookMapper {

    public static final String DEFAULT_OWNER_LABEL = "my book";

    public static Book toBookEntity(AddBookRequest request, User owner) {
        if (request == null || owner == null) {
            return null;
        }

        return Book.builder()
                .title(request.getTitle().trim())
                .author(request.getAuthor().trim())
                .description(trimToNull(request.getDescription()))
                .category(request.getCategory())
                .price(request.getPrice())
                .ownerLabel(DEFAULT_OWNER_LABEL)
                .owner(owner)
                .build();
    }

    public static MyBookshelfBookDto toMyBookshelfBookDto(Book book, BookTransfer transfer, UUID viewerId) {
        if (book == null) {
            return null;
        }

        return MyBookshelfBookDto.builder()
                .id(book.getId())
                .author(book.getAuthor())
                .title(book.getTitle())
                .description(book.getDescription())
                .category(book.getCategory())
                .price(book.getPrice())
                .ownerUsername(resolveOwnerDisplay(book, transfer, viewerId))
                .recipientUsername(resolveRecipientUsername(book, transfer, viewerId))
                .returnDeadline(transfer != null ? transfer.getReturnAt() : null)
                .deletable(isDeletable(book, transfer, viewerId))
                .returnable(isReturnable(transfer, viewerId))
                .build();
    }

    private static boolean isReturnable(BookTransfer transfer, UUID viewerId) {
        return transfer != null
                && transfer.getReceiver() != null
                && transfer.getReceiver().getId().equals(viewerId);
    }

    private static boolean isDeletable(Book book, BookTransfer transfer, UUID viewerId) {
        return transfer == null
                && book.getOwner() != null
                && book.getOwner().getId().equals(viewerId)
                && DEFAULT_OWNER_LABEL.equals(book.getOwnerLabel());
    }

    private static String resolveRecipientUsername(Book book, BookTransfer transfer, UUID viewerId) {
        if (book.getOwner() == null || !book.getOwner().getId().equals(viewerId)) {
            return "-";
        }
        if (transfer == null || transfer.getReceiver() == null) {
            return "-";
        }
        return transfer.getReceiver().getUsername();
    }

    private static String resolveOwnerDisplay(Book book, BookTransfer transfer, UUID viewerId) {
        if (transfer != null
                && transfer.getReceiver() != null
                && transfer.getReceiver().getId().equals(viewerId)) {
            return book.getOwner() != null ? book.getOwner().getUsername() : "-";
        }
        return resolveOwnerLabel(book);
    }

    private static String resolveOwnerLabel(Book book) {
        if (book.getOwnerLabel() != null && !book.getOwnerLabel().isBlank()) {
            return book.getOwnerLabel();
        }
        return book.getOwner() != null ? book.getOwner().getUsername() : null;
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
