package app.messageservice.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import app.messageservice.exception.EntityValidationException;

import static org.assertj.core.api.Assertions.assertThat;

class MessageExceptionHandlerTest {

    private final MessageExceptionHandler handler = new MessageExceptionHandler();

    @Test
    void handleNotFound_returns404() {
        var response = handler.handleNotFound();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Message not found");
    }

    @Test
    void handleAccessDenied_returns403() {
        var response = handler.handleAccessDenied();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Operation not allowed for this message");
    }

    @Test
    void handleValidation_returns400() {
        var ex = org.mockito.Mockito.mock(org.springframework.web.bind.MethodArgumentNotValidException.class);
        var bindingResult = org.mockito.Mockito.mock(org.springframework.validation.BindingResult.class);
        var fieldError = new org.springframework.validation.FieldError("request", "about", "Message subject is required");

        org.mockito.Mockito.when(ex.getBindingResult()).thenReturn(bindingResult);
        org.mockito.Mockito.when(bindingResult.getFieldErrors()).thenReturn(java.util.List.of(fieldError));

        var response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Message subject is required");
    }

    @Test
    void handleEntityValidation_returns400() {
        var response = handler.handleEntityValidation(new EntityValidationException("Message subject is required"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Message subject is required");
    }

    @Test
    void handleValidation_returnsDefaultMessageWhenFieldErrorsMissing() {
        var ex = org.mockito.Mockito.mock(org.springframework.web.bind.MethodArgumentNotValidException.class);
        var bindingResult = org.mockito.Mockito.mock(org.springframework.validation.BindingResult.class);

        org.mockito.Mockito.when(ex.getBindingResult()).thenReturn(bindingResult);
        org.mockito.Mockito.when(bindingResult.getFieldErrors()).thenReturn(java.util.List.of());

        var response = handler.handleValidation(ex);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid request");
    }

    @Test
    void handleResourceNotFound_returns404() {
        var response = handler.handleResourceNotFound();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("The requested resource was not found");
    }

    @Test
    void handleMethodNotSupported_returns405() {
        var response = handler.handleMethodNotSupported();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("This action is not supported");
    }

    @Test
    void handleUnexpected_returns500() {
        var response = handler.handleUnexpected();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
    }
}
