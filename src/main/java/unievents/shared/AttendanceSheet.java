package unievents.shared;

import java.util.List;

/**
 * Attendance sheet DTO returned by AttendanceManagementController / ManagementController (SD-09).
 */
public record AttendanceSheet(
        int eventId,
        String eventTitle,
        String checkInCode,
        List<AttendanceRow> students
) {
    public int getEventId() { return eventId; }
    public String getEventTitle() { return eventTitle; }
    public String getCheckInCode() { return checkInCode; }
    public List<AttendanceRow> getStudents() { return students; }
}
