package app.web;

import app.exception.BookNotAvailableForTransferException;
import app.exception.NotAuthenticatedException;
import app.exception.ReceiverNotFoundException;
import app.exception.SelfTransferException;
import app.model.dto.book.SendBookRequest;
import app.model.dto.user.UserSession;
import app.service.booktransfer.BookTransferService;
import app.service.user.UserService;
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

import java.time.LocalDate;
import java.util.UUID;

@Controller
@RequestMapping("/send-book")
public class SendBookController {

    private final BookTransferService bookTransferService;
    private final UserService userService;
    private final UserSessionService userSessionService;

    public SendBookController(BookTransferService bookTransferService,
                              UserService userService,
                              UserSessionService userSessionService) {
        this.bookTransferService = bookTransferService;
        this.userService = userService;
        this.userSessionService = userSessionService;
    }

    @GetMapping
    public String sendBookForm(HttpSession session, Model model) {
        UserSession userSession = requireUserSession(session);
        populateForm(model, userSession.getId());
        return "send-book";
    }

    @PostMapping
    public String submitSendBook(@Valid @ModelAttribute("sendBookRequest") SendBookRequest request,
                                 BindingResult bindingResult,
                                 HttpSession session,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        UserSession userSession = requireUserSession(session);

        if (bindingResult.hasErrors()) {
            populateForm(model, userSession.getId());
            return "send-book";
        }

        String receiverUsername = request.getReceiverUsername().trim();
        request.setReceiverUsername(receiverUsername);

        if (!userService.existsByUsername(receiverUsername)) {
            bindingResult.rejectValue("receiverUsername", "receiver.invalid", "Invalid username");
            populateForm(model, userSession.getId());
            return "send-book";
        }

        try {
            bookTransferService.sendBook(userSession.getId(), request);
        } catch (ReceiverNotFoundException ex) {
            bindingResult.rejectValue("receiverUsername", "receiver.invalid", "Invalid username");
            populateForm(model, userSession.getId());
            return "send-book";
        } catch (SelfTransferException ex) {
            bindingResult.rejectValue("receiverUsername", "receiver.self", "You cannot send a book to yourself");
            populateForm(model, userSession.getId());
            return "send-book";
        } catch (BookNotAvailableForTransferException ex) {
            bindingResult.rejectValue("bookId", "book.unavailable", "Selected book is not available for transfer");
            populateForm(model, userSession.getId());
            return "send-book";
        }

        redirectAttributes.addFlashAttribute("successMessage", "The book was sent successfully!");
        return "redirect:/send-book";
    }

    private void populateForm(Model model, UUID ownerId) {
        if (!model.containsAttribute("sendBookRequest")) {
            model.addAttribute("sendBookRequest", new SendBookRequest());
        }
        model.addAttribute("bookOptions", bookTransferService.getSendableBooks(ownerId));
        model.addAttribute("minReturnDate", LocalDate.now());
    }

    private UserSession requireUserSession(HttpSession session) {
        return userSessionService.get(session)
                .orElseThrow(NotAuthenticatedException::new);
    }
}
