package app.web;

import app.exception.MessageServiceUnavailableException;
import app.exception.NotAuthenticatedException;
import app.exception.ReceiverNotFoundException;
import app.exception.SelfMessageException;
import app.model.dto.message.MessageViewDto;
import app.model.dto.message.SendMessageFormRequest;
import app.model.dto.user.UserSession;
import app.service.message.MessageAppService;
import app.service.user.UserService;
import app.service.user.UserSessionService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

import java.util.Collections;
import java.util.UUID;

@Controller
@RequestMapping("/messages")
public class MessagesController {

    private final MessageAppService messageAppService;
    private final UserService userService;
    private final UserSessionService userSessionService;

    public MessagesController(MessageAppService messageAppService,
                              UserService userService,
                              UserSessionService userSessionService) {
        this.messageAppService = messageAppService;
        this.userService = userService;
        this.userSessionService = userSessionService;
    }

    @GetMapping
    public String messagesHub(HttpSession session, Model model) {
        UserSession userSession = requireUserSession(session);
        populateHubStats(model, userSession.getId());
        return "messages-hub";
    }

    @GetMapping("/new")
    public String newMessage(HttpSession session, Model model) {
        requireUserSession(session);

        if (!model.containsAttribute("sendMessageFormRequest")) {
            model.addAttribute("sendMessageFormRequest", new SendMessageFormRequest());
        }

        return "messages-new";
    }

    @GetMapping("/inbox")
    public String inbox(@PageableDefault(size = MessageAppService.INBOX_PAGE_SIZE) Pageable pageable,
                        HttpSession session,
                        Model model) {
        UserSession userSession = requireUserSession(session);
        populateInbox(model, userSession.getId(), pageable);
        return "messages-inbox";
    }

    @GetMapping("/inbox/{messageId}")
    public String viewInboxMessage(@PathVariable UUID messageId,
                                   @RequestParam(defaultValue = "0") int page,
                                   HttpSession session,
                                   Model model) {
        UserSession userSession = requireUserSession(session);
        model.addAttribute("page", page);

        try {
            model.addAttribute("message", messageAppService.viewInboxMessage(userSession.getId(), messageId));
        } catch (MessageServiceUnavailableException ex) {
            model.addAttribute("errorMessage", "Message not found or messaging service is unavailable.");
        }

        return "messages-inbox-view";
    }

    @GetMapping("/sent")
    public String sent(@PageableDefault(size = MessageAppService.SENT_PAGE_SIZE) Pageable pageable,
                       HttpSession session,
                       Model model) {
        UserSession userSession = requireUserSession(session);
        populateSent(model, userSession.getId(), pageable);
        return "messages-sent";
    }

    @PostMapping("/send")
    public String sendMessage(@Valid @ModelAttribute("sendMessageFormRequest") SendMessageFormRequest request,
                              BindingResult bindingResult,
                              HttpSession session,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        UserSession userSession = requireUserSession(session);

        if (bindingResult.hasErrors()) {
            return "messages-new";
        }

        String receiverUsername = request.getReceiverUsername().trim();
        request.setReceiverUsername(receiverUsername);

        if (userService.isUnknownUsername(receiverUsername)) {
            bindingResult.rejectValue("receiverUsername", "receiver.invalid", "Invalid username");
            return "messages-new";
        }

        try {
            messageAppService.sendMessage(userSession.getId(), request);
            redirectAttributes.addFlashAttribute("successMessage", "Message sent successfully.");
            return "redirect:/messages/new";
        } catch (ReceiverNotFoundException ex) {
            bindingResult.rejectValue("receiverUsername", "receiver.invalid", "Invalid username");
            return "messages-new";
        } catch (SelfMessageException ex) {
            bindingResult.rejectValue("receiverUsername", "receiver.self", "You cannot send a message to yourself");
            return "messages-new";
        } catch (MessageServiceUnavailableException ex) {
            model.addAttribute("serviceError", "Messaging service is unavailable. Please try again later.");
            return "messages-new";
        }
    }

    @PostMapping("/{messageId}/delete")
    public String deleteMessage(@PathVariable UUID messageId,
                                @RequestParam(required = false) String returnTo,
                                @RequestParam(defaultValue = "0") int page,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        UserSession userSession = requireUserSession(session);

        try {
            messageAppService.deleteMessage(userSession.getId(), messageId);
            redirectAttributes.addFlashAttribute("successMessage", "Message deleted successfully.");
        } catch (MessageServiceUnavailableException ex) {
            redirectAttributes.addFlashAttribute("serviceError", "Messaging service is unavailable. Please try again later.");
        }

        if ("sent".equals(returnTo)) {
            return "redirect:/messages/sent?page=" + page;
        }

        return "redirect:/messages/inbox?page=" + page;
    }

    private void populateHubStats(Model model, UUID userId) {
        try {
            model.addAttribute("unreadCount", messageAppService.getUnreadCount(userId));
            model.addAttribute("inboxCount", messageAppService.getInboxCount(userId));
        } catch (MessageServiceUnavailableException ex) {
            model.addAttribute("unreadCount", 0L);
            model.addAttribute("inboxCount", 0L);
            model.addAttribute("serviceError", "Messaging service is unavailable. Please try again later.");
        }
    }

    private void populateInbox(Model model, UUID userId, Pageable pageable) {
        try {
            Page<MessageViewDto> messagePage = messageAppService.getInboxPage(userId, pageable);
            model.addAttribute("messages", messagePage.getContent());
            model.addAttribute("page", messagePage.getNumber());
            model.addAttribute("pageSize", MessageAppService.INBOX_PAGE_SIZE);
            model.addAttribute("totalPages", messagePage.getTotalPages());
            model.addAttribute("hasPrevious", messagePage.hasPrevious());
            model.addAttribute("hasNext", messagePage.hasNext());
            model.addAttribute("unreadCount", messageAppService.getUnreadCount(userId));
        } catch (MessageServiceUnavailableException ex) {
            model.addAttribute("messages", Collections.emptyList());
            model.addAttribute("page", 0);
            model.addAttribute("pageSize", MessageAppService.INBOX_PAGE_SIZE);
            model.addAttribute("totalPages", 0);
            model.addAttribute("hasPrevious", false);
            model.addAttribute("hasNext", false);
            model.addAttribute("unreadCount", 0L);
            model.addAttribute("serviceError", "Messaging service is unavailable. Please try again later.");
        }
    }

    private void populateSent(Model model, UUID userId, Pageable pageable) {
        try {
            Page<MessageViewDto> messagePage = messageAppService.getSentPage(userId, pageable);
            model.addAttribute("messages", messagePage.getContent());
            model.addAttribute("page", messagePage.getNumber());
            model.addAttribute("pageSize", MessageAppService.SENT_PAGE_SIZE);
            model.addAttribute("totalPages", messagePage.getTotalPages());
            model.addAttribute("hasPrevious", messagePage.hasPrevious());
            model.addAttribute("hasNext", messagePage.hasNext());
        } catch (MessageServiceUnavailableException ex) {
            model.addAttribute("messages", Collections.emptyList());
            model.addAttribute("page", 0);
            model.addAttribute("pageSize", MessageAppService.SENT_PAGE_SIZE);
            model.addAttribute("totalPages", 0);
            model.addAttribute("hasPrevious", false);
            model.addAttribute("hasNext", false);
            model.addAttribute("serviceError", "Messaging service is unavailable. Please try again later.");
        }
    }

    private UserSession requireUserSession(HttpSession session) {
        return userSessionService.get(session)
                .orElseThrow(NotAuthenticatedException::new);
    }
}
