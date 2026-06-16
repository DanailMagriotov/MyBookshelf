package app.model.dto.book;

import app.model.entity.book.Category;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class MyBookshelfBookDto {

    private UUID id;
    private String author;
    private String title;
    private String description;
    private Category category;
    private BigDecimal price;
    private String ownerUsername;
    private String recipientUsername;
    private LocalDateTime returnDeadline;
    private boolean deletable;
    private boolean returnable;
    private boolean editable;
}
