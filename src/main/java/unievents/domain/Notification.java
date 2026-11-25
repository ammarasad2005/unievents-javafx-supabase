package unievents.domain;

import unievents.shared.DomainException;
import unievents.shared.NotificationType;

import java.time.LocalDateTime;

public class Notification {
    private int notificationId;
    private final User recipient;
    private final String message;
    private final NotificationType type;
    private final LocalDateTime createdAt;
    private boolean isRead;

    public Notification(User recipient, String message, NotificationType type) {
        this(0, recipient, message, type, LocalDateTime.now(), false);
    }

    public Notification(int notificationId, User recipient, String message,
                        NotificationType type, LocalDateTime createdAt, boolean isRead) {
        if (recipient == null) {
            throw new DomainException("Notification recipient is required.");
        }
        if (message == null || message.isBlank()) {
            throw new DomainException("Notification message cannot be empty.");
        }
        if (type == null) {
            throw new DomainException("Notification type is required.");
        }
        this.notificationId = notificationId;
        this.recipient = recipient;
        this.message = message.trim();
        this.type = type;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.isRead = isRead;
    }

    public void markRead() {
        this.isRead = true;
    }

    public void assignNotificationId(int id) {
        if (this.notificationId == 0 && id > 0) {
            this.notificationId = id;
        }
    }

    public int getNotificationId() { return notificationId; }
    public User getRecipient() { return recipient; }
    public String getMessage() { return message; }
    public NotificationType getType() { return type; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public boolean isRead() { return isRead; }
}
