package app.service.book;

import app.exception.AccessDeniedException;
import app.exception.BookNotAvailableForTransferException;
import app.exception.NotAuthenticatedException;
import app.exception.ReceiverNotFoundException;
import app.exception.SelfTransferException;
import app.mapper.book.BookMapper;
import app.model.dto.book.BookOptionDto;
import app.model.dto.book.SendBookRequest;
import app.model.entity.book.Book;
import app.model.entity.booktransfer.BookTransfer;
import app.model.entity.user.User;
import app.repository.book.BookRepository;
import app.repository.booktransfer.BookTransferRepository;
import app.repository.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BookTransferService {

    private final BookRepository bookRepository;
    private final BookTransferRepository bookTransferRepository;
    private final UserRepository userRepository;

    public BookTransferService(BookRepository bookRepository,
                               BookTransferRepository bookTransferRepository,
                               UserRepository userRepository) {
        this.bookRepository = bookRepository;
        this.bookTransferRepository = bookTransferRepository;
        this.userRepository = userRepository;
    }

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
            bookTransferRepository.saveAndFlush(transfer);
        } catch (DataIntegrityViolationException ex) {
            throw new BookNotAvailableForTransferException();
        }
    }

    @Transactional
    public void returnBook(UUID receiverId, UUID bookId) {
        BookTransfer transfer = bookTransferRepository.findByBook_IdAndReceiver_Id(bookId, receiverId)
                .orElseThrow(AccessDeniedException::new);

        Book book = transfer.getBook();
        User sender = transfer.getSender();
        if (book != null && sender != null) {
            book.setOwner(sender);
            book.setOwnerLabel(BookMapper.DEFAULT_OWNER_LABEL);
            bookRepository.save(book);
        }

        bookTransferRepository.delete(transfer);
        bookTransferRepository.flush();
    }
}
