package unievents.domain;

import unievents.shared.CheckInMethod;
import unievents.shared.DomainException;

import java.time.LocalDateTime;

public class Attendance {
    private int attendanceId;
    private final LocalDateTime checkedInAt;
    private final CheckInMethod method;

    public Attendance(CheckInMethod method) {
        this(0, LocalDateTime.now(), method);
    }

    public Attendance(int attendanceId, LocalDateTime checkedInAt, CheckInMethod method) {
        if (method == null) {
            throw new DomainException("Check-in method is required.");
        }
        this.attendanceId = attendanceId;
        this.checkedInAt = checkedInAt != null ? checkedInAt : LocalDateTime.now();
        this.method = method;
    }

    public void assignAttendanceId(int id) {
        if (this.attendanceId == 0 && id > 0) {
            this.attendanceId = id;
        }
    }

    public int getAttendanceId() { return attendanceId; }
    public LocalDateTime getCheckedInAt() { return checkedInAt; }
    public CheckInMethod getMethod() { return method; }
}
