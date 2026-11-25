package unievents.domain;

import unievents.shared.DomainException;
import unievents.shared.PasswordUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Abstract base class in the User hierarchy (Abstraction + Inheritance + Polymorphism).
 */
public abstract class User {
    private int userId;
    private final String fullName;
    private final String email;
    private final String passwordHash;
    private final boolean isActive;
    private final List<Notification> notifications = new ArrayList<>();

    protected User(int userId, String fullName, String email, String passwordHash, boolean isActive) {
        if (fullName == null || fullName.isBlank()) {
            throw new DomainException("User full name cannot be empty.");
        }
        if (email == null || email.isBlank()) {
            throw new DomainException("User email cannot be empty.");
        }
        this.userId = userId;
        this.fullName = fullName.trim();
        this.email = email.trim().toLowerCase();
        this.passwordHash = passwordHash != null ? passwordHash : "";
        this.isActive = isActive;
    }

    public int getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isActive() { return isActive; }

    public void assignUserId(int id) {
        if (this.userId == 0 && id > 0) {
            this.userId = id;
        }
    }

    public boolean verifyPassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            return false;
        }
        String hashed = PasswordUtil.sha256(rawPassword);
        return passwordHash.equalsIgnoreCase(hashed) || passwordHash.equals(rawPassword);
    }

    public void receiveNotification(Notification notification) {
        if (notification != null) {
            notifications.add(notification);
        }
    }

    public List<Notification> getNotifications() {
        return Collections.unmodifiableList(notifications);
    }

    /** Polymorphic role identifier from DCD. */
    public abstract String getRole();

    /** Polymorphic human-readable role label for UI header/navigation. */
    public abstract String getRoleDisplayName();

    /** Polymorphic profile descriptor without instanceof checks. */
    public abstract String getProfileSummary();

    /** Polymorphic double-dispatch hook for role-based workflows. */
    public abstract <T> T accept(UserRoleVisitor<T> visitor);
}
