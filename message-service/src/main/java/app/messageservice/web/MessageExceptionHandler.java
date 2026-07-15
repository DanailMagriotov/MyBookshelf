package app.messageservice.web;

import app.messageservice.exception.MessageAccessDeniedException;
import app.messageservice.exception.MessageNotFoundException;
import app.messageservice.model.dto.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class MessageExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Invalid request");
        return ResponseEntity.badRequest().body(ApiErrorResponse.builder().message(message).build());
    }

    @ExceptionHandler(MessageNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.builder().message("Message not found").build());
    }

    @ExceptionHandler(MessageAccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiErrorResponse.builder().message("Operation not allowed for this message").build());
    }
}
