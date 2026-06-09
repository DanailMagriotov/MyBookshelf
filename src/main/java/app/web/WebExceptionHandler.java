package app.web;

import app.exception.AccessDeniedException;
import app.exception.NotAuthenticatedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

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
}
