package app.event;

import app.model.entity.user.UserRole;

import java.util.UUID;

public record UserRoleChangedEvent(UUID targetUserId, UserRole newRole) {
}
