package unievents.shared;

import java.time.LocalDateTime;

/**
 * Participant row in an AttendanceReport (SD-10).
 */
public record Participant(
        String fullName,
        String rollNumber,
        RegistrationStatus status,
        LocalDateTime checkedInAt,
        CheckInMethod method
) {
    public String getFullName() { return fullName; }
    public String getRollNumber() { return rollNumber; }
    public RegistrationStatus getStatus() { return status; }
    public LocalDateTime getCheckedInAt() { return checkedInAt; }
    public CheckInMethod getMethod() { return method; }
}
