package app.repository.book;

import app.model.entity.book.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookRepository extends JpaRepository<Book, UUID> {

    void deleteByOwner_Id(UUID ownerId);

    @Query(value = """
            SELECT b FROM Book b
            WHERE b.owner.id = :userId
               OR EXISTS (
                   SELECT 1 FROM BookTransfer t
                   WHERE t.book = b AND t.receiver.id = :userId
               )
            """,
            countQuery = """
            SELECT COUNT(b) FROM Book b
            WHERE b.owner.id = :userId
               OR EXISTS (
                   SELECT 1 FROM BookTransfer t
                   WHERE t.book = b AND t.receiver.id = :userId
               )
            """)
    Page<Book> findVisibleBooksForUser(@Param("userId") UUID userId, Pageable pageable);

    @Query("""
            SELECT COUNT(b) FROM Book b
            WHERE b.owner.id = :userId
               OR EXISTS (
                   SELECT 1 FROM BookTransfer t
                   WHERE t.book = b AND t.receiver.id = :userId
               )
            """)
    long countVisibleBooksForUser(@Param("userId") UUID userId);

    Optional<Book> findByIdAndOwner_Id(UUID id, UUID ownerId);

    Optional<Book> findByIdAndOwner_IdAndOwnerLabel(UUID id, UUID ownerId, String ownerLabel);

    List<Book> findByOwner_IdAndOwnerLabelOrderByTitleAsc(UUID ownerId, String ownerLabel);
}
