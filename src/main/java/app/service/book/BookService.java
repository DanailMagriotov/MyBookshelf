package app.service.book;

import app.exception.AccessDeniedException;
import app.exception.NotAuthenticatedException;
import app.mapper.book.BookMapper;
import app.model.dto.book.AddBookRequest;
import app.model.dto.book.EditBookRequest;
import app.model.dto.book.MyBookshelfBookDto;
import app.model.entity.book.Book;
import app.model.entity.user.User;
import app.repository.book.BookRepository;
import app.repository.booktransfer.BookTransferRepository;
import app.repository.user.UserRepository;
import app.validation.EntityValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class BookService {

    public static final int BOOKS_PAGE_SIZE = 4;

    private static final Logger log = LoggerFactory.getLogger(BookService.class);

    private final BookRepository bookRepository;
    private final BookTransferRepository bookTransferRepository;
    private final UserRepository userRepository;
    private final EntityValidator entityValidator;

    public BookService(BookRepository bookRepository,
                       BookTransferRepository bookTransferRepository,
                       UserRepository userRepository,
                       EntityValidator entityValidator) {
        this.bookRepository = bookRepository;
        this.bookTransferRepository = bookTransferRepository;
        this.userRepository = userRepository;
        this.entityValidator = entityValidator;
    }

    @Transactional
    @CacheEvict(value = {"bookshelfCounts", "sendableBooks"}, allEntries = true)
    public void addBook(UUID ownerId, AddBookRequest request) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(NotAuthenticatedException::new);

        Book book = BookMapper.toBookEntity(request, owner);
        entityValidator.validate(book);
        bookRepository.save(book);
        log.info("User {} added book '{}'", ownerId, book.getTitle());
    }

    @Transactional
    @CacheEvict(value = {"bookshelfCounts", "sendableBooks"}, allEntries = true)
    public void deleteBook(UUID ownerId, UUID bookId) {
        Book book = bookRepository.findByIdAndOwner_Id(bookId, ownerId)
                .orElseThrow(NotAuthenticatedException::new);

        if (!BookMapper.DEFAULT_OWNER_LABEL.equals(book.getOwnerLabel())) {
            throw new AccessDeniedException();
        }

        if (bookTransferRepository.findByBook_Id(bookId).isPresent()) {
            throw new AccessDeniedException();
        }

        bookRepository.delete(book);
        log.info("User {} deleted book {}", ownerId, bookId);
    }

    public Book getBookForMetadataEdit(UUID ownerId, UUID bookId) {
        Book book = bookRepository.findByIdAndOwner_Id(bookId, ownerId)
                .orElseThrow(NotAuthenticatedException::new);

        assertBookMetadataEditable(book, bookId);
        return book;
    }

    public EditBookRequest toEditBookRequest(Book book) {
        return BookMapper.toEditBookRequest(book);
    }

    @Transactional
    @CacheEvict(value = {"bookshelfCounts", "sendableBooks"}, allEntries = true)
    public void updateBook(UUID ownerId, UUID bookId, EditBookRequest request) {
        Book book = bookRepository.findByIdAndOwner_Id(bookId, ownerId)
                .orElseThrow(NotAuthenticatedException::new);

        assertBookMetadataEditable(book, bookId);
        BookMapper.applyEditToBook(book, request);
        entityValidator.validate(book);
        bookRepository.save(book);
        log.info("User {} updated book {}", ownerId, bookId);
    }

    public Page<MyBookshelfBookDto> getBooksByOwner(UUID ownerId, Pageable pageable) {
        int pageNumber = Math.max(pageable.getPageNumber(), 0);
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.by("title").ascending();
        PageRequest pageRequest = PageRequest.of(pageNumber, BOOKS_PAGE_SIZE, sort);

        return bookRepository.findVisibleBooksForUser(ownerId, pageRequest)
                .map(book -> toMyBookshelfBookDto(book, ownerId));
    }

    public List<MyBookshelfBookDto> getAllVisibleBooksForExport(UUID ownerId) {
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, Sort.by("title").ascending());
        return bookRepository.findVisibleBooksForUser(ownerId, pageable)
                .map(book -> toMyBookshelfBookDto(book, ownerId))
                .getContent();
    }

    @Cacheable(value = "bookshelfCounts", key = "#userId")
    public long countVisibleBooksForUser(UUID userId) {
        return bookRepository.countVisibleBooksForUser(userId);
    }

    private MyBookshelfBookDto toMyBookshelfBookDto(Book book, UUID viewerId) {
        return BookMapper.toMyBookshelfBookDto(
                book,
                bookTransferRepository.findByBook_Id(book.getId()).orElse(null),
                viewerId
        );
    }

    private void assertBookMetadataEditable(Book book, UUID bookId) {
        if (!BookMapper.DEFAULT_OWNER_LABEL.equals(book.getOwnerLabel())) {
            throw new AccessDeniedException();
        }

        if (bookTransferRepository.findByBook_Id(bookId).isPresent()) {
            throw new AccessDeniedException();
        }
    }
}
