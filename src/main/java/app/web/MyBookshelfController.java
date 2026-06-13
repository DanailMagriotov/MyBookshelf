package app.web;

import app.exception.NotAuthenticatedException;
import app.model.dto.book.MyBookshelfBookDto;
import app.model.dto.user.UserSession;
import app.service.book.BookService;
import app.service.booktransfer.BookTransferService;
import app.service.user.UserSessionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/my-bookshelf")
public class MyBookshelfController {

    private final BookService bookService;
    private final BookTransferService bookTransferService;
    private final UserSessionService userSessionService;

    public MyBookshelfController(BookService bookService,
                                 BookTransferService bookTransferService,
                                 UserSessionService userSessionService) {
        this.bookService = bookService;
        this.bookTransferService = bookTransferService;
        this.userSessionService = userSessionService;
    }

    @GetMapping
    public String myBookshelf(@PageableDefault(size = BookService.BOOKS_PAGE_SIZE, sort = "title") Pageable pageable,
                              HttpSession session,
                              Model model) {
        UserSession userSession = requireUserSession(session);
        Page<MyBookshelfBookDto> bookPage = bookService.getBooksByOwner(userSession.getId(), pageable);

        model.addAttribute("books", bookPage.getContent());
        model.addAttribute("page", bookPage.getNumber());
        model.addAttribute("pageSize", BookService.BOOKS_PAGE_SIZE);
        model.addAttribute("totalPages", bookPage.getTotalPages());
        model.addAttribute("totalBooks", bookPage.getTotalElements());
        model.addAttribute("hasPrevious", bookPage.hasPrevious());
        model.addAttribute("hasNext", bookPage.hasNext());

        return "my-bookshelf";
    }

    @PostMapping("/delete/{bookId}")
    public String deleteBook(@PathVariable UUID bookId,
                             @RequestParam(defaultValue = "0") int page,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        UserSession userSession = requireUserSession(session);
        bookService.deleteBook(userSession.getId(), bookId);
        redirectAttributes.addFlashAttribute("successMessage", "Book deleted successfully.");
        return "redirect:/my-bookshelf?page=" + page;
    }

    @PostMapping("/return/{bookId}")
    public String returnBook(@PathVariable UUID bookId,
                             @RequestParam(defaultValue = "0") int page,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        UserSession userSession = requireUserSession(session);
        bookTransferService.returnBook(userSession.getId(), bookId);
        redirectAttributes.addFlashAttribute("successMessage", "Book returned successfully.");
        return "redirect:/my-bookshelf?page=" + page;
    }

    private UserSession requireUserSession(HttpSession session) {
        return userSessionService.get(session)
                .orElseThrow(NotAuthenticatedException::new);
    }
}
