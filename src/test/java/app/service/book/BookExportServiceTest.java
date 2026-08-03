package app.service.book;

import app.model.dto.book.MyBookshelfBookDto;
import app.model.entity.book.Category;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookExportServiceTest {

    @Mock
    private BookService bookService;

    @InjectMocks
    private BookExportService bookExportService;

    @Test
    void exportBookshelfToExcel_writesHeaderAndBookRow() throws Exception {
        UUID userId = UUID.randomUUID();
        MyBookshelfBookDto book = MyBookshelfBookDto.builder()
                .id(UUID.randomUUID())
                .title("Dune")
                .author("Herbert")
                .description("Sci-fi")
                .category(Category.FANTASY)
                .price(BigDecimal.TEN)
                .ownerUsername("my book")
                .recipientUsername("-")
                .returnDeadline(LocalDateTime.of(2026, 8, 1, 23, 59))
                .build();

        when(bookService.getAllVisibleBooksForExport(userId)).thenReturn(List.of(book));

        byte[] excelBytes = bookExportService.exportBookshelfToExcel(userId);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excelBytes))) {
            Sheet sheet = workbook.getSheet("My bookshelf");
            assertThat(sheet).isNotNull();

            Row header = sheet.getRow(0);
            assertThat(header.getCell(0).getStringCellValue()).isEqualTo("Title");
            assertThat(header.getCell(1).getStringCellValue()).isEqualTo("Author");

            Row data = sheet.getRow(1);
            assertThat(data.getCell(0).getStringCellValue()).isEqualTo("Dune");
            assertThat(data.getCell(1).getStringCellValue()).isEqualTo("Herbert");
            assertThat(data.getCell(3).getStringCellValue()).isEqualTo("FANTASY");
            assertThat(data.getCell(4).getNumericCellValue()).isEqualTo(10.0);
        }
    }

    @Test
    void exportBookshelfToExcel_handlesEmptyBookshelf() throws Exception {
        UUID userId = UUID.randomUUID();
        when(bookService.getAllVisibleBooksForExport(userId)).thenReturn(List.of());

        byte[] excelBytes = bookExportService.exportBookshelfToExcel(userId);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excelBytes))) {
            Sheet sheet = workbook.getSheet("My bookshelf");
            assertThat(sheet.getRow(0)).isNotNull();
            assertThat(sheet.getRow(1)).isNull();
        }
    }
}
