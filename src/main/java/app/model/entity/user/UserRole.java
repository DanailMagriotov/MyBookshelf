package app.model.entity.user;

public enum UserRole {
    MASTER_ADMIN,
    ADMIN,
    USER;

    public boolean isAdmin() {
        return this == MASTER_ADMIN || this == ADMIN;
    }
}
