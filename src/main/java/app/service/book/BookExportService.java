package app.service.book;

import app.model.dto.book.MyBookshelfBookDto;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class BookExportService {

    private static final Logger log = LoggerFactory.getLogger(BookExportService.class);
    private static final DateTimeFormatter DEADLINE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private static final String[] HEADERS = {
            "Title", "Author", "Description", "Category", "Price", "Owner", "Recipient", "Return deadline"
    };

    private final BookService bookService;

    public BookExportService(BookService bookService) {
        this.bookService = bookService;
    }

    public byte[] exportBookshelfToExcel(UUID userId) {
        List<MyBookshelfBookDto> books = bookService.getAllVisibleBooksForExport(userId);

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("My bookshelf");
            writeHeader(sheet);
            writeRows(sheet, books);
            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(output);
            log.info("User {} exported {} book(s) to Excel", userId, books.size());
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to export bookshelf to Excel", ex);
        }
    }

    private void writeHeader(Sheet sheet) {
        Row header = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            header.createCell(i).setCellValue(HEADERS[i]);
        }
    }

    private void writeRows(Sheet sheet, List<MyBookshelfBookDto> books) {
        int rowIndex = 1;
        for (MyBookshelfBookDto book : books) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(nullToEmpty(book.getTitle()));
            row.createCell(1).setCellValue(nullToEmpty(book.getAuthor()));
            row.createCell(2).setCellValue(nullToDash(book.getDescription()));
            row.createCell(3).setCellValue(book.getCategory() != null ? book.getCategory().name() : "-");
            row.createCell(4).setCellValue(book.getPrice() != null ? book.getPrice().doubleValue() : 0);
            row.createCell(5).setCellValue(nullToDash(book.getOwnerUsername()));
            row.createCell(6).setCellValue(nullToDash(book.getRecipientUsername()));
            row.createCell(7).setCellValue(book.getReturnDeadline() != null
                    ? DEADLINE_FORMAT.format(book.getReturnDeadline())
                    : "-");
        }
    }

    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }

    private static String nullToDash(String value) {
        return value != null && !value.isBlank() ? value : "-";
    }
}
