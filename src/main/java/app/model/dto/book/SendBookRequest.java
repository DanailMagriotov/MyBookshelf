package app.model.dto.book;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendBookRequest {

    @NotBlank(message = "Recipient username is required")
    @Size(max = 50, message = "Username must be at most 50 characters")
    private String receiverUsername;

    @NotNull(message = "Please select a book")
    private UUID bookId;

    @NotNull(message = "Return deadline is required")
    private LocalDate returnDeadline;
}
