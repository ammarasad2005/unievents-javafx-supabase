package unievents.shared;

import java.time.LocalDateTime;

/**
 * Summary item for open registration conflicts (SD-12).
 */
public record ConflictSummary(
        int conflictId,
        int eventId,
        String eventTitle,
        String fullName,
        ConflictType type,
        ConflictStatus status,
        LocalDateTime detectedAt
) {
    public int getConflictId() { return conflictId; }
    public int getEventId() { return eventId; }
    public String getEventTitle() { return eventTitle; }
    public String getFullName() { return fullName; }
    public ConflictType getType() { return type; }
    public ConflictStatus getStatus() { return status; }
    public LocalDateTime getDetectedAt() { return detectedAt; }
}
