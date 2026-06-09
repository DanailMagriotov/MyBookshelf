package app.model.dto.book;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class BookOptionDto {

    private UUID id;
    private String label;
}
