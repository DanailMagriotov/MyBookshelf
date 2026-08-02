package app.event;

import app.exception.MessageServiceUnavailableException;
import app.model.entity.user.UserRole;
import app.service.message.MessageAppService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RoleChangeNotificationListenerTest {

    @Mock
    private MessageAppService messageAppService;

    @InjectMocks
    private RoleChangeNotificationListener listener;

    @Test
    void onUserRoleChanged_sendsNotification() {
        UUID userId = UUID.randomUUID();

        listener.onUserRoleChanged(new UserRoleChangedEvent(userId, UserRole.ADMIN));

        verify(messageAppService).sendRoleChangeNotification(userId, UserRole.ADMIN);
    }

    @Test
    void onUserRoleChanged_swallowsUnavailableService() {
        UUID userId = UUID.randomUUID();
        doThrow(new MessageServiceUnavailableException(new RuntimeException("down")))
                .when(messageAppService).sendRoleChangeNotification(userId, UserRole.ADMIN);

        listener.onUserRoleChanged(new UserRoleChangedEvent(userId, UserRole.ADMIN));

        verify(messageAppService).sendRoleChangeNotification(userId, UserRole.ADMIN);
    }
}
