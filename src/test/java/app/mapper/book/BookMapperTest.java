package app.mapper.book;

import app.model.dto.book.AddBookRequest;
import app.model.dto.book.MyBookshelfBookDto;
import app.model.entity.book.Book;
import app.model.entity.book.Category;
import app.model.entity.booktransfer.BookTransfer;
import app.model.entity.user.Region;
import app.model.entity.user.User;
import app.model.entity.user.UserRole;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BookMapperTest {

    @Test
    void toBookEntity_returnsNullForMissingInput() {
        assertThat(BookMapper.toBookEntity(null, user(UUID.randomUUID()))).isNull();
        assertThat(BookMapper.toBookEntity(AddBookRequest.builder().build(), null)).isNull();
    }

    @Test
    void toBookEntity_trimsFields() {
        User owner = user(UUID.randomUUID());
        AddBookRequest request = AddBookRequest.builder()
                .title(" Title ")
                .author(" Author ")
                .description("  ")
                .category(Category.HISTORY)
                .price(BigDecimal.ONE)
                .build();

        Book book = BookMapper.toBookEntity(request, owner);

        assertThat(book.getTitle()).isEqualTo("Title");
        assertThat(book.getAuthor()).isEqualTo("Author");
        assertThat(book.getDescription()).isNull();
        assertThat(book.getOwnerLabel()).isEqualTo(BookMapper.DEFAULT_OWNER_LABEL);
    }

    @Test
    void toMyBookshelfBookDto_returnsNullForNullBook() {
        assertThat(BookMapper.toMyBookshelfBookDto(null, null, UUID.randomUUID())).isNull();
    }

    @Test
    void toMyBookshelfBookDto_usesOwnerLabelWhenPresent() {
        UUID ownerId = UUID.randomUUID();
        Book book = Book.builder()
                .id(UUID.randomUUID())
                .title("Title")
                .author("Author")
                .ownerLabel("my label")
                .owner(user(ownerId))
                .build();

        MyBookshelfBookDto dto = BookMapper.toMyBookshelfBookDto(book, null, ownerId);

        assertThat(dto.getOwnerUsername()).isEqualTo("my label");
    }

    @Test
    void toMyBookshelfBookDto_marksOwnedBookAsDeletable() {
        UUID ownerId = UUID.randomUUID();
        Book book = Book.builder()
                .id(UUID.randomUUID())
                .title("Title")
                .author("Author")
                .ownerLabel(BookMapper.DEFAULT_OWNER_LABEL)
                .owner(user(ownerId))
                .build();

        MyBookshelfBookDto dto = BookMapper.toMyBookshelfBookDto(book, null, ownerId);

        assertThat(dto.isDeletable()).isTrue();
        assertThat(dto.isReturnable()).isFalse();
        assertThat(dto.getRecipientUsername()).isEqualTo("-");
    }

    @Test
    void toMyBookshelfBookDto_marksBorrowedBookAsReturnable() {
        UUID ownerId = UUID.randomUUID();
        UUID borrowerId = UUID.randomUUID();
        User borrower = user(borrowerId);
        Book book = Book.builder()
                .id(UUID.randomUUID())
                .title("Title")
                .author("Author")
                .owner(user(ownerId))
                .build();
        BookTransfer transfer = BookTransfer.builder()
                .receiver(borrower)
                .returnAt(LocalDateTime.now().plusDays(2))
                .build();

        MyBookshelfBookDto dto = BookMapper.toMyBookshelfBookDto(book, transfer, borrowerId);

        assertThat(dto.isReturnable()).isTrue();
        assertThat(dto.isDeletable()).isFalse();
        assertThat(dto.getOwnerUsername()).isEqualTo(book.getOwner().getUsername());
    }

    @Test
    void toMyBookshelfBookDto_showsRecipientForOwnerView() {
        UUID ownerId = UUID.randomUUID();
        User owner = user(ownerId);
        User receiver = user(UUID.randomUUID());
        receiver.setUsername("borrower");
        Book book = Book.builder()
                .id(UUID.randomUUID())
                .title("Title")
                .author("Author")
                .owner(owner)
                .build();
        BookTransfer transfer = BookTransfer.builder()
                .receiver(receiver)
                .returnAt(LocalDateTime.now().plusDays(2))
                .build();

        MyBookshelfBookDto dto = BookMapper.toMyBookshelfBookDto(book, transfer, ownerId);

        assertThat(dto.isEditable()).isTrue();
        assertThat(dto.getRecipientUsername()).isEqualTo("borrower");
    }

    private static User user(UUID id) {
        return User.builder()
                .id(id)
                .username("user-" + id)
                .password("secret")
                .email("user@example.com")
                .role(UserRole.USER)
                .region(Region.SOFIA)
                .build();
    }
}
