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
import app.model.dto.book.BookOptionDto;
import app.model.dto.book.EditTransferRequest;
import app.model.dto.book.SendBookRequest;
import app.model.entity.book.Book;
import app.model.entity.booktransfer.BookTransfer;
import app.model.entity.user.User;
import app.repository.book.BookRepository;
import app.repository.booktransfer.BookTransferRepository;
import app.repository.user.UserRepository;
import app.service.message.MessageAppService;
import app.validation.EntityValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BookTransferService {

    private static final Logger log = LoggerFactory.getLogger(BookTransferService.class);

    private final BookRepository bookRepository;
    private final BookTransferRepository bookTransferRepository;
    private final UserRepository userRepository;
    private final MessageAppService messageAppService;
    private final EntityValidator entityValidator;
    private final ApplicationEventPublisher eventPublisher;

    public BookTransferService(BookRepository bookRepository,
                               BookTransferRepository bookTransferRepository,
                               UserRepository userRepository,
                               MessageAppService messageAppService,
                               EntityValidator entityValidator,
                               ApplicationEventPublisher eventPublisher) {
        this.bookRepository = bookRepository;
        this.bookTransferRepository = bookTransferRepository;
        this.userRepository = userRepository;
        this.messageAppService = messageAppService;
        this.entityValidator = entityValidator;
        this.eventPublisher = eventPublisher;
    }

    @Cacheable(value = "sendableBooks", key = "#ownerId")
    public List<BookOptionDto> getSendableBooks(UUID ownerId) {
        return bookRepository.findByOwner_IdAndOwnerLabelOrderByTitleAsc(ownerId, BookMapper.DEFAULT_OWNER_LABEL)
                .stream()
                .filter(book -> !bookTransferRepository.existsByBook_Id(book.getId()))
                .map(book -> BookOptionDto.builder()
                        .id(book.getId())
                        .label(book.getAuthor() + " - " + book.getTitle())
                        .build())
                .toList();
    }

    @Transactional
    @CacheEvict(value = {"bookshelfCounts", "sendableBooks"}, allEntries = true)
    public void sendBook(UUID senderId, SendBookRequest request) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(NotAuthenticatedException::new);

        String receiverUsername = request.getReceiverUsername().trim();
        User receiver = userRepository.findByUsername(receiverUsername)
                .orElseThrow(ReceiverNotFoundException::new);

        if (sender.getId().equals(receiver.getId())) {
            throw new SelfTransferException();
        }

        Book book = bookRepository.findByIdAndOwner_IdAndOwnerLabel(
                        request.getBookId(),
                        senderId,
                        BookMapper.DEFAULT_OWNER_LABEL)
                .orElseThrow(BookNotAvailableForTransferException::new);

        if (bookTransferRepository.existsByBook_Id(book.getId())) {
            throw new BookNotAvailableForTransferException();
        }

        LocalDateTime returnAt = request.getReturnDeadline().atTime(23, 59);

        BookTransfer transfer = BookTransfer.builder()
                .sender(sender)
                .receiver(receiver)
                .book(book)
                .createdAt(LocalDateTime.now())
                .returnAt(returnAt)
                .build();

        try {
            entityValidator.validate(transfer);
            bookTransferRepository.saveAndFlush(transfer);
            log.info("User {} sent book {} to {}", senderId, book.getId(), receiverUsername);
            eventPublisher.publishEvent(new BookSentEvent(
                    senderId, receiver.getId(), book.getId(), book.getTitle()));
        } catch (DataIntegrityViolationException ex) {
            throw new BookNotAvailableForTransferException();
        }
    }

    @Transactional
    @CacheEvict(value = {"bookshelfCounts", "sendableBooks"}, allEntries = true)
    public void returnBook(UUID receiverId, UUID bookId) {
        BookTransfer transfer = bookTransferRepository.findByBook_IdAndReceiver_Id(bookId, receiverId)
                .orElseThrow(AccessDeniedException::new);

        Book book = transfer.getBook();
        User sender = transfer.getSender();
        if (book != null && sender != null) {
            book.setOwner(sender);
            book.setOwnerLabel(BookMapper.DEFAULT_OWNER_LABEL);
            entityValidator.validate(book);
            bookRepository.save(book);
        }

        bookTransferRepository.delete(transfer);
        bookTransferRepository.flush();
        log.info("User {} returned book {}", receiverId, bookId);
    }

    public Book getOwnedTransferBook(UUID ownerId, UUID bookId) {
        return findOwnedTransfer(ownerId, bookId).getBook();
    }

    public EditTransferRequest getEditTransferRequest(UUID ownerId, UUID bookId) {
        BookTransfer transfer = findOwnedTransfer(ownerId, bookId);
        return EditTransferRequest.builder()
                .returnDeadline(transfer.getReturnAt().toLocalDate())
                .build();
    }

    public LocalDate getMinReturnDate(UUID ownerId, UUID bookId) {
        return findOwnedTransfer(ownerId, bookId).getReturnAt().toLocalDate();
    }

    @Transactional
    @CacheEvict(value = {"bookshelfCounts", "sendableBooks"}, allEntries = true)
    public void updateReturnDeadline(UUID ownerId, UUID bookId, LocalDate returnDeadline) {
        BookTransfer transfer = findOwnedTransfer(ownerId, bookId);
        LocalDate currentDeadline = transfer.getReturnAt().toLocalDate();

        if (returnDeadline.isBefore(currentDeadline)) {
            throw new InvalidReturnDeadlineException();
        }

        transfer.setReturnAt(returnDeadline.atTime(23, 59));
        transfer.setUpdatedAt(LocalDateTime.now());
        transfer.setOverdueReminderSent(false);
        entityValidator.validate(transfer);
        bookTransferRepository.save(transfer);
        log.info("User {} updated return deadline for book {} to {}", ownerId, bookId, returnDeadline);
    }

    @Transactional
    public int sendOverdueTransferReminders() {
        List<BookTransfer> overdueTransfers = bookTransferRepository
                .findByReturnAtBeforeAndOverdueReminderSentFalse(LocalDateTime.now());
        int notifiedCount = 0;

        for (BookTransfer transfer : overdueTransfers) {
            Book book = transfer.getBook();
            User sender = transfer.getSender();
            User receiver = transfer.getReceiver();

            if (book == null || sender == null || receiver == null) {
                continue;
            }

            String bookLabel = book.getAuthor() + " - " + book.getTitle();
            String receiverMessage = "Hello, the return deadline for the book \""
                    + bookLabel
                    + "\" has expired. Please return it to its owner.";
            String senderMessage = "The book \"" + bookLabel + "\" has an expired return deadline.";

            try {
                messageAppService.sendSystemMessage(receiver.getId(), receiverMessage);
                messageAppService.sendSystemMessage(sender.getId(), senderMessage);
            } catch (MessageServiceUnavailableException ex) {
                log.error("Failed to send overdue return reminders for transfer {}", transfer.getId());
                continue;
            }

            transfer.setOverdueReminderSent(true);
            bookTransferRepository.save(transfer);
            notifiedCount++;
            log.info("Sent overdue return reminders for book {} to users {} and {}",
                    book.getId(), receiver.getId(), sender.getId());
        }

        return notifiedCount;
    }

    private BookTransfer findOwnedTransfer(UUID ownerId, UUID bookId) {
        return bookTransferRepository.findByBook_IdAndSender_Id(bookId, ownerId)
                .orElseThrow(AccessDeniedException::new);
    }
}
