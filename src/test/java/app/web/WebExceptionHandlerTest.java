package app.web;

import app.exception.EntityValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import static org.assertj.core.api.Assertions.assertThat;

class WebExceptionHandlerTest {

    private final WebExceptionHandler handler = new WebExceptionHandler();

    @Test
    void handleNotAuthenticated_redirectsToLogin() {
        assertThat(handler.handleNotAuthenticated()).isEqualTo("redirect:/login");
    }

    @Test
    void handleAccessDenied_redirectsToHome() {
        assertThat(handler.handleAccessDenied()).isEqualTo("redirect:/home");
    }

    @Test
    void handleMethodNotSupported_returnsErrorViewWithAttributes() {
        Model model = new ConcurrentModel();

        String view = handler.handleMethodNotSupported(model);

        assertThat(view).isEqualTo("error");
        assertThat(model.getAttribute("status")).isEqualTo(405);
        assertThat(model.getAttribute("message")).isEqualTo("This action is not supported.");
    }

    @Test
    void handleEntityValidation_returnsErrorViewWithAttributes() {
        Model model = new ConcurrentModel();

        String view = handler.handleEntityValidation(new EntityValidationException("Title is required"), model);

        assertThat(view).isEqualTo("error");
        assertThat(model.getAttribute("status")).isEqualTo(400);
        assertThat(model.getAttribute("message")).isEqualTo("Title is required");
    }

    @Test
    void handleResourceNotFound_returnsErrorViewWithAttributes() {
        Model model = new ConcurrentModel();

        String view = handler.handleResourceNotFound(model);

        assertThat(view).isEqualTo("error");
        assertThat(model.getAttribute("status")).isEqualTo(404);
        assertThat(model.getAttribute("message")).isEqualTo("The requested page was not found.");
    }
}
