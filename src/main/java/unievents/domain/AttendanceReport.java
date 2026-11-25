package unievents.domain;

import unievents.shared.DomainException;
import unievents.shared.Participant;
import unievents.shared.RegistrationStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AttendanceReport {
    private int reportId;
    private final ClubOrganizer organizer;
    private final Event event;
    private final LocalDateTime generatedAt;
    private final int registeredCount;
    private final int attendedCount;

    public AttendanceReport(ClubOrganizer organizer, Event event, int registeredCount, int attendedCount) {
        this(0, organizer, event, LocalDateTime.now(), registeredCount, attendedCount);
    }

    public AttendanceReport(int reportId, ClubOrganizer organizer, Event event,
                            LocalDateTime generatedAt, int registeredCount, int attendedCount) {
        if (organizer == null) {
            throw new DomainException("Organizer is required to generate an attendance report.");
        }
        if (event == null) {
            throw new DomainException("Event is required to generate an attendance report.");
        }
        this.reportId = reportId;
        this.organizer = organizer;
        this.event = event;
        this.generatedAt = generatedAt != null ? generatedAt : LocalDateTime.now();
        this.registeredCount = registeredCount;
        this.attendedCount = attendedCount;
    }

    public void assignReportId(int id) {
        if (this.reportId == 0 && id > 0) {
            this.reportId = id;
        }
    }

    /**
     * SD-10: Loops over event registrations and maps each non-cancelled participant via Registration.toParticipant().
     */
    public List<Participant> getParticipants() {
        List<Participant> participants = new ArrayList<>();
        for (Registration registration : event.getRegistrations()) {
            if (registration.getStatus() != RegistrationStatus.CANCELLED) {
                participants.add(registration.toParticipant());
            }
        }
        return Collections.unmodifiableList(participants);
    }

    public double getAttendanceRatePercent() {
        if (registeredCount <= 0) {
            return 0.0;
        }
        return Math.round((attendedCount * 1000.0) / registeredCount) / 10.0;
    }

    public int getReportId() { return reportId; }
    public ClubOrganizer getOrganizer() { return organizer; }
    public Event getEvent() { return event; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public int getRegisteredCount() { return registeredCount; }
    public int getAttendedCount() { return attendedCount; }
}
