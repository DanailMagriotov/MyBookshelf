package app.testsupport;

import app.model.dto.user.UserSession;
import app.model.entity.user.Region;
import app.model.entity.user.UserRole;
import app.service.user.UserSessionService;
import app.web.WebExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.AbstractView;
import org.springframework.web.servlet.view.InternalResourceView;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

public final class WebTestSupport {

    private WebTestSupport() {
    }

    public static UserSession userSession() {
        return userSession(UserRole.USER);
    }

    public static UserSession adminSession() {
        return userSession(UserRole.ADMIN);
    }

    public static UserSession userSession(UserRole role) {
        return userSession(UUID.randomUUID(), role);
    }

    public static UserSession userSession(UUID id, UserRole role) {
        return UserSession.builder()
                .id(id)
                .username("alice")
                .email("alice@example.com")
                .role(role)
                .region(Region.SOFIA)
                .build();
    }

    public static MockHttpSession sessionWith(UserSession userSession) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(UserSessionService.SESSION_ATTRIBUTE, userSession);
        return session;
    }

    public static StandaloneMockMvcBuilder standaloneWithPageable(Object... controllers) {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        return standaloneSetup(controllers)
                .setValidator(validator)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new WebExceptionHandler())
                .setViewResolvers((viewName, locale) -> resolveView(viewName));
    }

    private static View resolveView(String viewName) {
        if (viewName.startsWith("redirect:")) {
            return new RedirectView(viewName.substring("redirect:".length()));
        }
        if (viewName.startsWith("forward:")) {
            return new InternalResourceView(viewName.substring("forward:".length()));
        }
        return noopView();
    }

    private static View noopView() {
        return new AbstractView() {
            @Override
            protected void renderMergedOutputModel(@NonNull Map<String, Object> model,
                                                   @NonNull HttpServletRequest request,
                                                   @NonNull HttpServletResponse response) {
                // Standalone MockMvc only needs a resolvable view name.
            }

            @Override
            public String getContentType() {
                return "text/html";
            }
        };
    }
}
