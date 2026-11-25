package unievents.shared;

import java.time.LocalDateTime;

/**
 * Registration summary DTO returned in SD-03 (My Registrations) and SD-12 (Conflict Details).
 */
public record RegistrationSummary(
        int registrationId,
        int eventId,
        String eventTitle,
        String clubName,
        int studentId,
        String studentName,
        String rollNumber,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        String venue,
        RegistrationStatus status,
        Integer waitlistPosition,
        LocalDateTime registeredAt,
        EventStatus eventStatus
) {
    public int getRegistrationId() { return registrationId; }
    public int getEventId() { return eventId; }
    public String getEventTitle() { return eventTitle; }
    public String getClubName() { return clubName; }
    public int getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public String getRollNumber() { return rollNumber; }
    public LocalDateTime getStartDateTime() { return startDateTime; }
    public LocalDateTime getEndDateTime() { return endDateTime; }
    public String getVenue() { return venue; }
    public RegistrationStatus getStatus() { return status; }
    public Integer getWaitlistPosition() { return waitlistPosition; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public EventStatus getEventStatus() { return eventStatus; }
}
