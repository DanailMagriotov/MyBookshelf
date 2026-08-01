package app.web;

import app.exception.NotAuthenticatedException;
import app.model.dto.book.EditBookRequest;
import app.model.dto.user.UserSession;
import app.model.entity.book.Book;
import app.model.entity.book.Category;
import app.service.book.BookService;
import app.service.user.UserSessionService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/edit-book")
public class EditBookController {

    private final BookService bookService;
    private final UserSessionService userSessionService;

    public EditBookController(BookService bookService, UserSessionService userSessionService) {
        this.bookService = bookService;
        this.userSessionService = userSessionService;
    }

    @ModelAttribute("categories")
    public Category[] categories() {
        return Category.values();
    }

    @GetMapping("/{bookId}")
    public String editBookForm(@PathVariable UUID bookId,
                               @RequestParam(defaultValue = "0") int page,
                               HttpSession session,
                               Model model) {
        UserSession userSession = requireUserSession(session);
        Book book = bookService.getBookForMetadataEdit(userSession.getId(), bookId);

        model.addAttribute("bookId", bookId);
        model.addAttribute("page", page);

        if (!model.containsAttribute("editBookRequest")) {
            model.addAttribute("editBookRequest", bookService.toEditBookRequest(book));
        }

        return "edit-book";
    }

    @PostMapping("/{bookId}")
    public String submitEditBook(@PathVariable UUID bookId,
                                 @RequestParam(defaultValue = "0") int page,
                                 @Valid @ModelAttribute("editBookRequest") EditBookRequest request,
                                 BindingResult bindingResult,
                                 HttpSession session,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        UserSession userSession = requireUserSession(session);

        if (bindingResult.hasErrors()) {
            model.addAttribute("bookId", bookId);
            model.addAttribute("page", page);
            return "edit-book";
        }

        bookService.updateBook(userSession.getId(), bookId, request);
        redirectAttributes.addFlashAttribute("successMessage", "Book updated successfully.");
        return "redirect:/my-bookshelf?page=" + page;
    }

    private UserSession requireUserSession(HttpSession session) {
        return userSessionService.get(session)
                .orElseThrow(NotAuthenticatedException::new);
    }
}
