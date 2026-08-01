package app.web;

import app.exception.BookNotAvailableForTransferException;
import app.exception.ReceiverNotFoundException;
import app.exception.SelfTransferException;
import app.model.dto.book.EditTransferRequest;
import app.model.dto.book.MyBookshelfBookDto;
import app.model.entity.book.Book;
import app.model.entity.book.Category;
import app.service.book.BookService;
import app.service.booktransfer.BookTransferService;
import app.service.user.UserService;
import app.service.user.UserSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static app.testsupport.WebTestSupport.sessionWith;
import static app.testsupport.WebTestSupport.standaloneWithPageable;
import static app.testsupport.WebTestSupport.userSession;
import static app.model.entity.user.UserRole.USER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class BookControllersApiTest {

    @Mock
    private BookService bookService;

    @Mock
    private BookTransferService bookTransferService;

    @Mock
    private UserService userService;

    @Mock
    private UserSessionService userSessionService;

    private MockMvc addBookMockMvc;
    private MockMvc editBookMockMvc;
    private MockMvc myBookshelfMockMvc;
    private MockMvc sendBookMockMvc;

    private final UUID userId = UUID.randomUUID();
    private final UUID bookId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        addBookMockMvc = standaloneWithPageable(new AddBookController(bookService, userSessionService)).build();
        editBookMockMvc = standaloneWithPageable(new EditBookController(bookService, userSessionService)).build();
        myBookshelfMockMvc = standaloneWithPageable(
                new MyBookshelfController(bookService, bookTransferService, userSessionService)).build();
        sendBookMockMvc = standaloneWithPageable(
                new SendBookController(bookTransferService, userService, userSessionService)).build();
    }

    private app.model.dto.user.UserSession authenticatedSession() {
        return userSession(userId, USER);
    }

    @Test
    void addBookPage_returnsForm() throws Exception {
        var session = authenticatedSession();
        when(userSessionService.get(any())).thenReturn(Optional.of(session));

        addBookMockMvc.perform(get("/add-book").session(sessionWith(session)))
                .andExpect(status().isOk())
                .andExpect(view().name("add-book"));
    }

    @Test
    void addBookSubmit_withValidData_redirectsToBookshelf() throws Exception {
        var session = authenticatedSession();
        when(userSessionService.get(any())).thenReturn(Optional.of(session));
        doNothing().when(bookService).addBook(eq(userId), any());

        addBookMockMvc.perform(post("/add-book")
                        .session(sessionWith(session))
                        .param("title", "Dune")
                        .param("author", "Herbert")
                        .param("category", "FANTASY")
                        .param("price", "12.50"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/my-bookshelf"))
                .andExpect(flash().attribute("successMessage", "Book added successfully."));
    }

    @Test
    void addBookSubmit_withValidationErrors_returnsForm() throws Exception {
        var session = authenticatedSession();
        when(userSessionService.get(any())).thenReturn(Optional.of(session));

        addBookMockMvc.perform(post("/add-book")
                        .session(sessionWith(session))
                        .param("title", "")
                        .param("author", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("add-book"));
    }

    @Test
    void editBookPage_returnsPrefilledForm() throws Exception {
        var session = authenticatedSession();
        var book = Book.builder()
                .id(bookId)
                .title("Dune")
                .author("Herbert")
                .category(Category.FANTASY)
                .build();

        when(userSessionService.get(any())).thenReturn(Optional.of(session));
        when(bookService.getBookForMetadataEdit(userId, bookId)).thenReturn(book);
        when(bookService.toEditBookRequest(book)).thenReturn(
                app.model.dto.book.EditBookRequest.builder()
                        .title("Dune")
                        .author("Herbert")
                        .category(Category.FANTASY)
                        .build());

        editBookMockMvc.perform(get("/edit-book/{bookId}", bookId)
                        .session(sessionWith(session))
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("edit-book"));
    }

    @Test
    void editBookSubmit_withValidData_redirectsToBookshelf() throws Exception {
        var session = authenticatedSession();
        when(userSessionService.get(any())).thenReturn(Optional.of(session));
        doNothing().when(bookService).updateBook(eq(userId), eq(bookId), any());

        editBookMockMvc.perform(post("/edit-book/{bookId}", bookId)
                        .session(sessionWith(session))
                        .param("page", "2")
                        .param("title", "Dune")
                        .param("author", "Herbert")
                        .param("category", "FANTASY")
                        .param("price", "12.50"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/my-bookshelf?page=2"))
                .andExpect(flash().attribute("successMessage", "Book updated successfully."));
    }

    @Test
    void editBookSubmit_withValidationErrors_returnsForm() throws Exception {
        var session = authenticatedSession();
        when(userSessionService.get(any())).thenReturn(Optional.of(session));

        editBookMockMvc.perform(post("/edit-book/{bookId}", bookId)
                        .session(sessionWith(session))
                        .param("title", "")
                        .param("author", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("edit-book"));
    }

    @Test
    void myBookshelfPage_returnsPagedBooks() throws Exception {
        var session = authenticatedSession();
        var bookDto = MyBookshelfBookDto.builder().id(bookId).title("Dune").author("Herbert").build();
        var page = new PageImpl<>(List.of(bookDto), PageRequest.of(0, BookService.BOOKS_PAGE_SIZE), 1);

        when(userSessionService.get(any())).thenReturn(Optional.of(session));
        when(bookService.getBooksByOwner(eq(userId), any())).thenReturn(page);

        myBookshelfMockMvc.perform(get("/my-bookshelf").session(sessionWith(session)))
                .andExpect(status().isOk())
                .andExpect(view().name("my-bookshelf"));
    }

    @Test
    void deleteBook_redirectsToBookshelf() throws Exception {
        var session = authenticatedSession();
        when(userSessionService.get(any())).thenReturn(Optional.of(session));
        doNothing().when(bookService).deleteBook(userId, bookId);

        myBookshelfMockMvc.perform(post("/my-bookshelf/delete/{bookId}", bookId)
                        .session(sessionWith(session))
                        .param("page", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/my-bookshelf?page=1"));
    }

    @Test
    void returnBook_redirectsToBookshelf() throws Exception {
        var session = authenticatedSession();
        when(userSessionService.get(any())).thenReturn(Optional.of(session));
        doNothing().when(bookTransferService).returnBook(userId, bookId);

        myBookshelfMockMvc.perform(post("/my-bookshelf/return/{bookId}", bookId)
                        .session(sessionWith(session)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/my-bookshelf?page=0"));
    }

    @Test
    void editTransferForm_returnsEditView() throws Exception {
        var session = authenticatedSession();
        var book = Book.builder().id(bookId).title("Dune").author("Herbert").build();
        var minDate = LocalDate.now().plusDays(1);

        when(userSessionService.get(any())).thenReturn(Optional.of(session));
        when(bookTransferService.getOwnedTransferBook(userId, bookId)).thenReturn(book);
        when(bookTransferService.getMinReturnDate(userId, bookId)).thenReturn(minDate);
        when(bookTransferService.getEditTransferRequest(userId, bookId))
                .thenReturn(EditTransferRequest.builder().returnDeadline(minDate).build());

        myBookshelfMockMvc.perform(get("/my-bookshelf/edit/{bookId}", bookId).session(sessionWith(session)))
                .andExpect(status().isOk())
                .andExpect(view().name("edit-transfer"));
    }

    @Test
    void updateTransfer_withValidDeadline_redirectsToBookshelf() throws Exception {
        var session = authenticatedSession();
        var book = Book.builder().id(bookId).title("Dune").author("Herbert").build();
        var minDate = LocalDate.now();
        var newDeadline = minDate.plusDays(5);

        when(userSessionService.get(any())).thenReturn(Optional.of(session));
        when(bookTransferService.getMinReturnDate(userId, bookId)).thenReturn(minDate);
        when(bookTransferService.getOwnedTransferBook(userId, bookId)).thenReturn(book);
        doNothing().when(bookTransferService).updateReturnDeadline(userId, bookId, newDeadline);

        myBookshelfMockMvc.perform(post("/my-bookshelf/edit/{bookId}", bookId)
                        .session(sessionWith(session))
                        .param("returnDeadline", newDeadline.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/my-bookshelf?page=0"));
    }

    @Test
    void updateTransfer_withEarlyDeadline_returnsEditView() throws Exception {
        var session = authenticatedSession();
        var book = Book.builder().id(bookId).title("Dune").author("Herbert").build();
        var minDate = LocalDate.now().plusDays(3);
        var tooEarly = minDate.minusDays(1);

        when(userSessionService.get(any())).thenReturn(Optional.of(session));
        when(bookTransferService.getMinReturnDate(userId, bookId)).thenReturn(minDate);
        when(bookTransferService.getOwnedTransferBook(userId, bookId)).thenReturn(book);

        myBookshelfMockMvc.perform(post("/my-bookshelf/edit/{bookId}", bookId)
                        .session(sessionWith(session))
                        .param("returnDeadline", tooEarly.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("edit-transfer"));
    }

    @Test
    void sendBookPage_returnsForm() throws Exception {
        var session = authenticatedSession();
        when(userSessionService.get(any())).thenReturn(Optional.of(session));
        when(bookTransferService.getSendableBooks(userId)).thenReturn(List.of());

        sendBookMockMvc.perform(get("/send-book").session(sessionWith(session)))
                .andExpect(status().isOk())
                .andExpect(view().name("send-book"));
    }

    @Test
    void sendBookSubmit_withValidData_redirectsToSendBook() throws Exception {
        var session = authenticatedSession();
        var deadline = LocalDate.now().plusDays(7);

        when(userSessionService.get(any())).thenReturn(Optional.of(session));
        when(userService.isUnknownUsername("bob")).thenReturn(false);
        doNothing().when(bookTransferService).sendBook(eq(userId), any());

        sendBookMockMvc.perform(post("/send-book")
                        .session(sessionWith(session))
                        .param("receiverUsername", "bob")
                        .param("bookId", bookId.toString())
                        .param("returnDeadline", deadline.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/send-book"));
    }

    @Test
    void sendBookSubmit_withUnknownReceiver_returnsForm() throws Exception {
        var session = authenticatedSession();
        var deadline = LocalDate.now().plusDays(7);

        when(userSessionService.get(any())).thenReturn(Optional.of(session));
        when(userService.isUnknownUsername("unknown")).thenReturn(true);
        when(bookTransferService.getSendableBooks(userId)).thenReturn(List.of());

        sendBookMockMvc.perform(post("/send-book")
                        .session(sessionWith(session))
                        .param("receiverUsername", "unknown")
                        .param("bookId", bookId.toString())
                        .param("returnDeadline", deadline.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("send-book"));
    }

    @Test
    void sendBookSubmit_withSelfTransfer_returnsForm() throws Exception {
        var session = authenticatedSession();
        var deadline = LocalDate.now().plusDays(7);

        when(userSessionService.get(any())).thenReturn(Optional.of(session));
        when(userService.isUnknownUsername("alice")).thenReturn(false);
        when(bookTransferService.getSendableBooks(userId)).thenReturn(List.of());
        doThrow(new SelfTransferException()).when(bookTransferService).sendBook(eq(userId), any());

        sendBookMockMvc.perform(post("/send-book")
                        .session(sessionWith(session))
                        .param("receiverUsername", "alice")
                        .param("bookId", bookId.toString())
                        .param("returnDeadline", deadline.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("send-book"));
    }

    @Test
    void sendBookSubmit_withUnavailableBook_returnsForm() throws Exception {
        var session = authenticatedSession();
        var deadline = LocalDate.now().plusDays(7);

        when(userSessionService.get(any())).thenReturn(Optional.of(session));
        when(userService.isUnknownUsername("bob")).thenReturn(false);
        when(bookTransferService.getSendableBooks(userId)).thenReturn(List.of());
        doThrow(new BookNotAvailableForTransferException())
                .when(bookTransferService).sendBook(eq(userId), any());

        sendBookMockMvc.perform(post("/send-book")
                        .session(sessionWith(session))
                        .param("receiverUsername", "bob")
                        .param("bookId", bookId.toString())
                        .param("returnDeadline", deadline.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("send-book"));
    }

    @Test
    void sendBookSubmit_withReceiverNotFound_returnsForm() throws Exception {
        var session = authenticatedSession();
        var deadline = LocalDate.now().plusDays(7);

        when(userSessionService.get(any())).thenReturn(Optional.of(session));
        when(userService.isUnknownUsername("bob")).thenReturn(false);
        when(bookTransferService.getSendableBooks(userId)).thenReturn(List.of());
        doThrow(new ReceiverNotFoundException()).when(bookTransferService).sendBook(eq(userId), any());

        sendBookMockMvc.perform(post("/send-book")
                        .session(sessionWith(session))
                        .param("receiverUsername", "bob")
                        .param("bookId", bookId.toString())
                        .param("returnDeadline", deadline.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("send-book"));

        verify(bookTransferService).sendBook(eq(userId), any());
    }
}
