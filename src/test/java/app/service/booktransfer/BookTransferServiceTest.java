package app.service.booktransfer;

import app.event.BookSentEvent;
import app.exception.AccessDeniedException;
import app.exception.BookNotAvailableForTransferException;
import app.exception.InvalidReturnDeadlineException;
import app.exception.MessageServiceUnavailableException;
import app.exception.NotAuthenticatedException;
import app.exception.ReceiverNotFoundException;
import app.exception.SelfTransferException;
import app.mapper.book.BookMapper;
import app.model.dto.book.SendBookRequest;
import app.model.entity.book.Book;
import app.model.entity.booktransfer.BookTransfer;
import app.model.entity.user.Region;
import app.model.entity.user.User;
import app.model.entity.user.UserRole;
import app.repository.book.BookRepository;
import app.repository.booktransfer.BookTransferRepository;
import app.repository.user.UserRepository;
import app.service.message.MessageAppService;
import app.validation.EntityValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookTransferServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookTransferRepository bookTransferRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MessageAppService messageAppService;

    @Mock
    private EntityValidator entityValidator;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private BookTransferService bookTransferService;

    @Test
    void getSendableBooks_excludesTransferredBooks() {
        UUID ownerId = UUID.randomUUID();
        Book available = book(UUID.randomUUID(), "Author A", "Title A");
        Book transferred = book(UUID.randomUUID(), "Author B", "Title B");

        when(bookRepository.findByOwner_IdAndOwnerLabelOrderByTitleAsc(ownerId, BookMapper.DEFAULT_OWNER_LABEL))
                .thenReturn(List.of(available, transferred));
        when(bookTransferRepository.existsByBook_Id(available.getId())).thenReturn(false);
        when(bookTransferRepository.existsByBook_Id(transferred.getId())).thenReturn(true);

        var result = bookTransferService.getSendableBooks(ownerId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLabel()).isEqualTo("Author A - Title A");
    }

    @Test
    void sendBook_createsTransfer() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        User sender = user(senderId, "sender");
        User receiver = user(receiverId, "receiver");
        Book book = book(bookId, "Author", "Title");

        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(userRepository.findByUsername("receiver")).thenReturn(Optional.of(receiver));
        when(bookRepository.findByIdAndOwner_IdAndOwnerLabel(bookId, senderId, BookMapper.DEFAULT_OWNER_LABEL))
                .thenReturn(Optional.of(book));
        when(bookTransferRepository.existsByBook_Id(bookId)).thenReturn(false);

        SendBookRequest request = SendBookRequest.builder()
                .bookId(bookId)
                .receiverUsername("receiver")
                .returnDeadline(LocalDate.now().plusDays(7))
                .build();

        bookTransferService.sendBook(senderId, request);

        verify(entityValidator).validate(any(BookTransfer.class));
        verify(bookTransferRepository).saveAndFlush(any(BookTransfer.class));
        verify(eventPublisher).publishEvent(new BookSentEvent(senderId, receiverId, bookId, "Title"));
    }

    @Test
    void sendBook_throwsWhenSenderMissing() {
        UUID senderId = UUID.randomUUID();
        when(userRepository.findById(senderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookTransferService.sendBook(senderId, SendBookRequest.builder()
                .bookId(UUID.randomUUID())
                .receiverUsername("receiver")
                .returnDeadline(LocalDate.now().plusDays(1))
                .build()))
                .isInstanceOf(NotAuthenticatedException.class);
    }

    @Test
    void sendBook_throwsWhenReceiverMissing() {
        UUID senderId = UUID.randomUUID();
        when(userRepository.findById(senderId)).thenReturn(Optional.of(user(senderId, "sender")));
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookTransferService.sendBook(senderId, SendBookRequest.builder()
                .bookId(UUID.randomUUID())
                .receiverUsername("missing")
                .returnDeadline(LocalDate.now().plusDays(1))
                .build()))
                .isInstanceOf(ReceiverNotFoundException.class);
    }

    @Test
    void sendBook_throwsOnSelfTransfer() {
        UUID senderId = UUID.randomUUID();
        User sender = user(senderId, "sender");
        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(userRepository.findByUsername("sender")).thenReturn(Optional.of(sender));

        assertThatThrownBy(() -> bookTransferService.sendBook(senderId, SendBookRequest.builder()
                .bookId(UUID.randomUUID())
                .receiverUsername("sender")
                .returnDeadline(LocalDate.now().plusDays(1))
                .build()))
                .isInstanceOf(SelfTransferException.class);
    }

    @Test
    void sendBook_throwsWhenBookUnavailable() {
        UUID senderId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        when(userRepository.findById(senderId)).thenReturn(Optional.of(user(senderId, "sender")));
        when(userRepository.findByUsername("receiver")).thenReturn(Optional.of(user(UUID.randomUUID(), "receiver")));
        when(bookRepository.findByIdAndOwner_IdAndOwnerLabel(bookId, senderId, BookMapper.DEFAULT_OWNER_LABEL))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookTransferService.sendBook(senderId, SendBookRequest.builder()
                .bookId(bookId)
                .receiverUsername("receiver")
                .returnDeadline(LocalDate.now().plusDays(1))
                .build()))
                .isInstanceOf(BookNotAvailableForTransferException.class);
    }

    @Test
    void returnBook_restoresOwnerAndDeletesTransfer() {
        UUID receiverId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        User sender = user(senderId, "sender");
        Book book = book(bookId, "Author", "Title");
        BookTransfer transfer = BookTransfer.builder()
                .sender(sender)
                .receiver(user(receiverId, "receiver"))
                .book(book)
                .build();

        when(bookTransferRepository.findByBook_IdAndReceiver_Id(bookId, receiverId)).thenReturn(Optional.of(transfer));

        bookTransferService.returnBook(receiverId, bookId);

        assertThat(book.getOwner()).isEqualTo(sender);
        assertThat(book.getOwnerLabel()).isEqualTo(BookMapper.DEFAULT_OWNER_LABEL);
        verify(bookRepository).save(book);
        verify(bookTransferRepository).delete(transfer);
        verify(bookTransferRepository).flush();
    }

    @Test
    void returnBook_throwsWhenNotReceiver() {
        UUID receiverId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        when(bookTransferRepository.findByBook_IdAndReceiver_Id(bookId, receiverId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookTransferService.returnBook(receiverId, bookId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateReturnDeadline_throwsWhenEarlierDate() {
        UUID ownerId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        BookTransfer transfer = BookTransfer.builder()
                .returnAt(LocalDate.now().plusDays(5).atTime(23, 59))
                .build();

        when(bookTransferRepository.findByBook_IdAndSender_Id(bookId, ownerId)).thenReturn(Optional.of(transfer));

        assertThatThrownBy(() -> bookTransferService.updateReturnDeadline(ownerId, bookId, LocalDate.now().plusDays(1)))
                .isInstanceOf(InvalidReturnDeadlineException.class);
    }

    @Test
    void updateReturnDeadline_resetsOverdueReminderFlag() {
        UUID ownerId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        BookTransfer transfer = BookTransfer.builder()
                .returnAt(LocalDate.now().plusDays(2).atTime(23, 59))
                .overdueReminderSent(true)
                .build();

        when(bookTransferRepository.findByBook_IdAndSender_Id(bookId, ownerId)).thenReturn(Optional.of(transfer));

        bookTransferService.updateReturnDeadline(ownerId, bookId, LocalDate.now().plusDays(10));

        assertThat(transfer.isOverdueReminderSent()).isFalse();
        verify(bookTransferRepository).save(transfer);
    }

    @Test
    void sendBook_throwsWhenTransferAlreadyExists() {
        UUID senderId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        when(userRepository.findById(senderId)).thenReturn(Optional.of(user(senderId, "sender")));
        when(userRepository.findByUsername("receiver")).thenReturn(Optional.of(user(UUID.randomUUID(), "receiver")));
        when(bookRepository.findByIdAndOwner_IdAndOwnerLabel(bookId, senderId, BookMapper.DEFAULT_OWNER_LABEL))
                .thenReturn(Optional.of(book(bookId, "Author", "Title")));
        when(bookTransferRepository.existsByBook_Id(bookId)).thenReturn(true);

        assertThatThrownBy(() -> bookTransferService.sendBook(senderId, SendBookRequest.builder()
                .bookId(bookId)
                .receiverUsername("receiver")
                .returnDeadline(LocalDate.now().plusDays(1))
                .build()))
                .isInstanceOf(BookNotAvailableForTransferException.class);
    }

    @Test
    void sendBook_throwsOnDataIntegrityViolation() {
        UUID senderId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        when(userRepository.findById(senderId)).thenReturn(Optional.of(user(senderId, "sender")));
        when(userRepository.findByUsername("receiver")).thenReturn(Optional.of(user(UUID.randomUUID(), "receiver")));
        when(bookRepository.findByIdAndOwner_IdAndOwnerLabel(bookId, senderId, BookMapper.DEFAULT_OWNER_LABEL))
                .thenReturn(Optional.of(book(bookId, "Author", "Title")));
        when(bookTransferRepository.existsByBook_Id(bookId)).thenReturn(false);
        when(bookTransferRepository.saveAndFlush(any(BookTransfer.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> bookTransferService.sendBook(senderId, SendBookRequest.builder()
                .bookId(bookId)
                .receiverUsername("receiver")
                .returnDeadline(LocalDate.now().plusDays(1))
                .build()))
                .isInstanceOf(BookNotAvailableForTransferException.class);
    }

    @Test
    void getOwnedTransferBook_returnsBookFromTransfer() {
        UUID ownerId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        Book book = book(bookId, "Author", "Title");
        BookTransfer transfer = BookTransfer.builder().book(book).build();

        when(bookTransferRepository.findByBook_IdAndSender_Id(bookId, ownerId)).thenReturn(Optional.of(transfer));

        assertThat(bookTransferService.getOwnedTransferBook(ownerId, bookId)).isEqualTo(book);
    }

    @Test
    void getEditTransferRequest_returnsCurrentDeadline() {
        UUID ownerId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        LocalDate deadline = LocalDate.now().plusDays(3);
        BookTransfer transfer = BookTransfer.builder()
                .returnAt(deadline.atTime(23, 59))
                .build();

        when(bookTransferRepository.findByBook_IdAndSender_Id(bookId, ownerId)).thenReturn(Optional.of(transfer));

        assertThat(bookTransferService.getEditTransferRequest(ownerId, bookId).getReturnDeadline())
                .isEqualTo(deadline);
    }

    @Test
    void getMinReturnDate_returnsCurrentDeadline() {
        UUID ownerId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        LocalDate deadline = LocalDate.now().plusDays(4);
        BookTransfer transfer = BookTransfer.builder()
                .returnAt(deadline.atTime(23, 59))
                .build();

        when(bookTransferRepository.findByBook_IdAndSender_Id(bookId, ownerId)).thenReturn(Optional.of(transfer));

        assertThat(bookTransferService.getMinReturnDate(ownerId, bookId)).isEqualTo(deadline);
    }

    @Test
    void sendOverdueTransferReminders_skipsIncompleteTransfer() {
        BookTransfer transfer = BookTransfer.builder()
                .id(UUID.randomUUID())
                .returnAt(LocalDateTime.now().minusDays(1))
                .build();

        when(bookTransferRepository.findByReturnAtBeforeAndOverdueReminderSentFalse(any(LocalDateTime.class)))
                .thenReturn(List.of(transfer));

        assertThat(bookTransferService.sendOverdueTransferReminders()).isZero();
        verify(messageAppService, never()).sendSystemMessage(any(), any());
    }

    @Test
    void sendOverdueTransferReminders_sendsMessagesAndMarksTransfer() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        User sender = user(senderId, "sender");
        User receiver = user(receiverId, "receiver");
        Book book = book(UUID.randomUUID(), "Author", "Title");
        BookTransfer transfer = BookTransfer.builder()
                .id(UUID.randomUUID())
                .sender(sender)
                .receiver(receiver)
                .book(book)
                .returnAt(LocalDateTime.now().minusDays(1))
                .overdueReminderSent(false)
                .build();

        when(bookTransferRepository.findByReturnAtBeforeAndOverdueReminderSentFalse(any(LocalDateTime.class)))
                .thenReturn(List.of(transfer));

        int count = bookTransferService.sendOverdueTransferReminders();

        assertThat(count).isOne();
        verify(messageAppService).sendSystemMessage(eq(receiverId), any(String.class));
        verify(messageAppService).sendSystemMessage(eq(senderId), any(String.class));
        assertThat(transfer.isOverdueReminderSent()).isTrue();
        verify(bookTransferRepository).save(transfer);
    }

    @Test
    void sendOverdueTransferReminders_skipsWhenMessageServiceUnavailable() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        BookTransfer transfer = BookTransfer.builder()
                .id(UUID.randomUUID())
                .sender(user(senderId, "sender"))
                .receiver(user(receiverId, "receiver"))
                .book(book(UUID.randomUUID(), "Author", "Title"))
                .returnAt(LocalDateTime.now().minusDays(1))
                .build();

        when(bookTransferRepository.findByReturnAtBeforeAndOverdueReminderSentFalse(any(LocalDateTime.class)))
                .thenReturn(List.of(transfer));
        doThrow(MessageServiceUnavailableException.class)
                .when(messageAppService).sendSystemMessage(eq(receiverId), any(String.class));

        int count = bookTransferService.sendOverdueTransferReminders();

        assertThat(count).isZero();
        assertThat(transfer.isOverdueReminderSent()).isFalse();
        verify(bookTransferRepository, never()).save(transfer);
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

    private static Book book(UUID id, String author, String title) {
        return Book.builder()
                .id(id)
                .author(author)
                .title(title)
                .ownerLabel(BookMapper.DEFAULT_OWNER_LABEL)
                .build();
    }
}
