package app.mapper.book;

import app.model.dto.book.BookDto;
import app.model.entity.book.Book;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class BookMapper {

    public static BookDto toBookDto(Book book) {
        if (book == null) {
            return null;
        }
        return BookDto.builder()
                .id(book.getId())
                .price(book.getPrice())
                .author(book.getAuthor())
                .title(book.getTitle())
                .description(book.getDescription())
                .category(book.getCategory())
                .owner(book.getOwner())
                .build();
    }
}
