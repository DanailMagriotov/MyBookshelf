package app.web;

import app.exception.NotAuthenticatedException;
import app.model.dto.book.AddBookRequest;
import app.model.dto.user.UserSession;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/add-book")
public class AddBookController {

    private final BookService bookService;
    private final UserSessionService userSessionService;

    public AddBookController(BookService bookService, UserSessionService userSessionService) {
        this.bookService = bookService;
        this.userSessionService = userSessionService;
    }

    @ModelAttribute("categories")
    public Category[] categories() {
        return Category.values();
    }

    @GetMapping
    public String addBook(HttpSession session, Model model) {
        requireUserSession(session);

        if (!model.containsAttribute("addBookRequest")) {
            model.addAttribute("addBookRequest", new AddBookRequest());
        }

        return "add-book";
    }

    @PostMapping
    public String submitAddBook(@Valid @ModelAttribute("addBookRequest") AddBookRequest request,
                                BindingResult bindingResult,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        UserSession userSession = requireUserSession(session);

        if (bindingResult.hasErrors()) {
            return "add-book";
        }

        bookService.addBook(userSession.getId(), request);
        redirectAttributes.addFlashAttribute("successMessage", "Book added successfully.");
        return "redirect:/my-bookshelf";
    }

    private UserSession requireUserSession(HttpSession session) {
        return userSessionService.get(session)
                .orElseThrow(NotAuthenticatedException::new);
    }
}
