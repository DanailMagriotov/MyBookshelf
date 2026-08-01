package app.service.book;

import app.exception.AccessDeniedException;
import app.exception.NotAuthenticatedException;
import app.mapper.book.BookMapper;
import app.model.dto.book.AddBookRequest;
import app.model.dto.book.EditBookRequest;
import app.model.entity.book.Book;
import app.model.entity.book.Category;
import app.model.entity.booktransfer.BookTransfer;
import app.model.entity.user.Region;
import app.model.entity.user.User;
import app.model.entity.user.UserRole;
import app.repository.book.BookRepository;
import app.repository.booktransfer.BookTransferRepository;
import app.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookTransferRepository bookTransferRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BookService bookService;

    @Test
    void addBook_savesBookForOwner() {
        UUID ownerId = UUID.randomUUID();
        User owner = user(ownerId, "alice");
        AddBookRequest request = AddBookRequest.builder()
                .title(" Title ")
                .author(" Author ")
                .category(Category.FANTASY)
                .price(BigDecimal.TEN)
                .build();

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));

        bookService.addBook(ownerId, request);

        ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("Title");
        assertThat(captor.getValue().getOwner()).isEqualTo(owner);
    }

    @Test
    void addBook_throwsWhenOwnerMissing() {
        UUID ownerId = UUID.randomUUID();
        when(userRepository.findById(ownerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.addBook(ownerId, AddBookRequest.builder()
                .title("Book")
                .author("Author")
                .category(Category.OTHER)
                .build()))
                .isInstanceOf(NotAuthenticatedException.class);
    }

    @Test
    void deleteBook_deletesOwnedBookWithoutTransfer() {
        UUID ownerId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        Book book = book(bookId, ownerId, BookMapper.DEFAULT_OWNER_LABEL);

        when(bookRepository.findByIdAndOwner_Id(bookId, ownerId)).thenReturn(Optional.of(book));
        when(bookTransferRepository.findByBook_Id(bookId)).thenReturn(Optional.empty());

        bookService.deleteBook(ownerId, bookId);

        verify(bookRepository).delete(book);
    }

    @Test
    void deleteBook_throwsWhenBookIsTransferred() {
        UUID ownerId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        Book book = book(bookId, ownerId, BookMapper.DEFAULT_OWNER_LABEL);

        when(bookRepository.findByIdAndOwner_Id(bookId, ownerId)).thenReturn(Optional.of(book));
        when(bookTransferRepository.findByBook_Id(bookId)).thenReturn(Optional.of(new BookTransfer()));

        assertThatThrownBy(() -> bookService.deleteBook(ownerId, bookId))
                .isInstanceOf(AccessDeniedException.class);
        verify(bookRepository, never()).delete(any());
    }

    @Test
    void deleteBook_throwsWhenOwnerLabelIsNotDefault() {
        UUID ownerId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        Book book = book(bookId, ownerId, "borrowed");

        when(bookRepository.findByIdAndOwner_Id(bookId, ownerId)).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> bookService.deleteBook(ownerId, bookId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateBook_updatesOwnedBookWithoutTransfer() {
        UUID ownerId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        Book book = book(bookId, ownerId, BookMapper.DEFAULT_OWNER_LABEL);
        EditBookRequest request = EditBookRequest.builder()
                .title(" New Title ")
                .author(" New Author ")
                .category(Category.SCIENCE)
                .price(BigDecimal.valueOf(5))
                .build();

        when(bookRepository.findByIdAndOwner_Id(bookId, ownerId)).thenReturn(Optional.of(book));
        when(bookTransferRepository.findByBook_Id(bookId)).thenReturn(Optional.empty());

        bookService.updateBook(ownerId, bookId, request);

        ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("New Title");
        assertThat(captor.getValue().getAuthor()).isEqualTo("New Author");
        assertThat(captor.getValue().getCategory()).isEqualTo(Category.SCIENCE);
    }

    @Test
    void updateBook_throwsWhenBookIsTransferred() {
        UUID ownerId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        Book book = book(bookId, ownerId, BookMapper.DEFAULT_OWNER_LABEL);

        when(bookRepository.findByIdAndOwner_Id(bookId, ownerId)).thenReturn(Optional.of(book));
        when(bookTransferRepository.findByBook_Id(bookId)).thenReturn(Optional.of(new BookTransfer()));

        assertThatThrownBy(() -> bookService.updateBook(ownerId, bookId, EditBookRequest.builder()
                .title("Title")
                .author("Author")
                .category(Category.OTHER)
                .build()))
                .isInstanceOf(AccessDeniedException.class);
        verify(bookRepository, never()).save(any());
    }

    @Test
    void getBookForMetadataEdit_throwsWhenOwnerLabelIsNotDefault() {
        UUID ownerId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        Book book = book(bookId, ownerId, "borrowed");

        when(bookRepository.findByIdAndOwner_Id(bookId, ownerId)).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> bookService.getBookForMetadataEdit(ownerId, bookId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getBooksByOwner_returnsMappedPage() {
        UUID ownerId = UUID.randomUUID();
        Book book = book(UUID.randomUUID(), ownerId, BookMapper.DEFAULT_OWNER_LABEL);
        Page<Book> page = new PageImpl<>(List.of(book));

        when(bookRepository.findVisibleBooksForUser(eq(ownerId), any(PageRequest.class))).thenReturn(page);
        when(bookTransferRepository.findByBook_Id(book.getId())).thenReturn(Optional.empty());

        Page<?> result = bookService.getBooksByOwner(ownerId, PageRequest.of(0, BookService.BOOKS_PAGE_SIZE));

        assertThat(result.getTotalElements()).isOne();
        assertThat(result.getContent().get(0)).isNotNull();
    }

    @Test
    void countVisibleBooksForUser_delegatesToRepository() {
        UUID userId = UUID.randomUUID();
        when(bookRepository.countVisibleBooksForUser(userId)).thenReturn(3L);

        assertThat(bookService.countVisibleBooksForUser(userId)).isEqualTo(3L);
    }

    private static User user(UUID id, String username) {
        return User.builder()
                .id(id)
                .username(username)
                .password("secret")
                .email(username + "@example.com")
                .role(UserRole.USER)
                .region(Region.SOFIA)
                .build();
    }

    private static Book book(UUID id, UUID ownerId, String ownerLabel) {
        return Book.builder()
                .id(id)
                .title("Title")
                .author("Author")
                .ownerLabel(ownerLabel)
                .owner(user(ownerId, "owner"))
                .build();
    }
}
