package unievents.application;

import unievents.domain.AttendanceReport;
import unievents.domain.ClubOrganizer;
import unievents.domain.Event;
import unievents.domain.Registration;
import unievents.infrastructure.persistence.DataHandler;
import unievents.shared.AttendanceSheet;
import unievents.shared.DomainException;

import java.util.List;

/**
 * UC9 & UC10 / SD-09 & SD-10: Organizer Manual Attendance Recording & Attendance Report Generation.
 */
public class AttendanceManagementController {
    private final DataHandler dataHandler;
    private final ClubOrganizer organizer;

    public AttendanceManagementController(DataHandler dataHandler, ClubOrganizer organizer) {
        this.dataHandler = dataHandler;
        this.organizer = organizer;
    }

    public AttendanceSheet getAttendanceSheet(int eventId) {
        ClubOrganizer freshOrg = requireFreshOrganizer();
        Event event = EventManagementController.requireOwnedEvent(dataHandler, freshOrg, eventId);
        return new AttendanceSheet(
                event.getEventId(),
                event.getTitle(),
                event.getCheckInCode(),
                event.getAttendanceSheet()
        );
    }

    public int recordAttendance(int eventId, List<Integer> registrationIds) {
        ClubOrganizer freshOrg = requireFreshOrganizer();
        Event event = EventManagementController.requireOwnedEvent(dataHandler, freshOrg, eventId);
        List<Registration> updated = event.recordAttendance(registrationIds);
        dataHandler.saveRegistrations(updated);
        return updated.size();
    }

    public AttendanceReport generateAttendanceReport(int eventId) {
        ClubOrganizer freshOrg = requireFreshOrganizer();
        Event event = EventManagementController.requireOwnedEvent(dataHandler, freshOrg, eventId);
        AttendanceReport report = event.generateReport(freshOrg);
        dataHandler.saveAttendanceReport(report);
        return report;
    }

    private ClubOrganizer requireFreshOrganizer() {
        ClubOrganizer fresh = dataHandler.findOrganizerById(organizer.getUserId());
        if (fresh == null) {
            throw new DomainException("Organizer session could not be verified.");
        }
        return fresh;
    }
}
