package app.event;

import app.exception.MessageServiceUnavailableException;
import app.service.message.MessageAppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class RoleChangeNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(RoleChangeNotificationListener.class);

    private final MessageAppService messageAppService;

    public RoleChangeNotificationListener(MessageAppService messageAppService) {
        this.messageAppService = messageAppService;
    }

    @EventListener
    public void onUserRoleChanged(UserRoleChangedEvent event) {
        try {
            messageAppService.sendRoleChangeNotification(event.targetUserId(), event.newRole());
        } catch (MessageServiceUnavailableException ex) {
            log.error("Role changed for user {} but notification could not be sent", event.targetUserId());
        }
    }
}
