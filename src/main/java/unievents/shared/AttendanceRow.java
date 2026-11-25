package unievents.shared;

import java.time.LocalDateTime;

/**
 * Single row on an event attendance sheet (SD-09).
 */
public record AttendanceRow(
        int registrationId,
        int studentId,
        String fullName,
        String rollNumber,
        RegistrationStatus status,
        boolean alreadyCheckedIn,
        LocalDateTime checkedInAt,
        CheckInMethod method
) {
    public int getRegistrationId() { return registrationId; }
    public int getStudentId() { return studentId; }
    public String getFullName() { return fullName; }
    public String getRollNumber() { return rollNumber; }
    public RegistrationStatus getStatus() { return status; }
    public boolean isAlreadyCheckedIn() { return alreadyCheckedIn; }
    public LocalDateTime getCheckedInAt() { return checkedInAt; }
    public CheckInMethod getMethod() { return method; }
}
