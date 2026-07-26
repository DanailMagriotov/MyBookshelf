package app.model.entity.booktransfer;

import app.model.entity.book.Book;
import app.model.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(
        name = "book_transfer",
        uniqueConstraints = @UniqueConstraint(name = "uk_book_transfer_book", columnNames = "book_id")
)
public class BookTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(optional = false)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @ManyToOne(optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "return_time", nullable = false)
    private LocalDateTime returnAt;

    @Column(name = "overdue_reminder_sent", nullable = false)
    @ColumnDefault("0")
    @Builder.Default
    private boolean overdueReminderSent = false;
}
