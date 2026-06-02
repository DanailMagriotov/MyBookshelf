package app.model.dto.book;

import app.model.entity.book.Category;
import app.model.entity.user.User;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class BookDto {

    private UUID id;
    private String author;
    private String title;
    private String description;
    private Category category;
    private BigDecimal price;
    private User owner;
}
