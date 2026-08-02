package app.web;

import app.exception.AccessDeniedException;
import app.exception.EntityValidationException;
import app.exception.NotAuthenticatedException;
import org.springframework.ui.Model;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class WebExceptionHandler {

    @ExceptionHandler(NotAuthenticatedException.class)
    public String handleNotAuthenticated() {
        return "redirect:/login";
    }

    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied() {
        return "redirect:/home";
    }

    @ExceptionHandler(EntityValidationException.class)
    public String handleEntityValidation(EntityValidationException ex, Model model) {
        model.addAttribute("status", 400);
        model.addAttribute("message", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public String handleMethodNotSupported(Model model) {
        model.addAttribute("status", 405);
        model.addAttribute("message", "This action is not supported.");
        return "error";
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public String handleResourceNotFound(Model model) {
        model.addAttribute("status", 404);
        model.addAttribute("message", "The requested page was not found.");
        return "error";
    }
}
