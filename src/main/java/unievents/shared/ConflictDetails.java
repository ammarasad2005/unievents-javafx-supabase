package unievents.shared;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Detailed conflict view for Admin resolution (SD-12).
 */
public record ConflictDetails(
        int conflictId,
        int eventId,
        String eventTitle,
        ConflictType type,
        ConflictStatus status,
        LocalDateTime detectedAt,
        int capacity,
        int registeredCount,
        List<RegistrationSummary> concernedRegistrations,
        List<RegistrationSummary> registrationHistory,
        List<RegistrationSummary> waitlist,
        ResolutionType resolution,
        String reason,
        LocalDateTime resolvedAt
) {
    public int getConflictId() { return conflictId; }
    public int getEventId() { return eventId; }
    public String getEventTitle() { return eventTitle; }
    public ConflictType getType() { return type; }
    public ConflictStatus getStatus() { return status; }
    public LocalDateTime getDetectedAt() { return detectedAt; }
    public int getCapacity() { return capacity; }
    public int getRegisteredCount() { return registeredCount; }
    public List<RegistrationSummary> getConcernedRegistrations() { return concernedRegistrations; }
    public List<RegistrationSummary> getRegistrationHistory() { return registrationHistory; }
    public List<RegistrationSummary> getWaitlist() { return waitlist; }
    public ResolutionType getResolution() { return resolution; }
    public String getReason() { return reason; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
}
