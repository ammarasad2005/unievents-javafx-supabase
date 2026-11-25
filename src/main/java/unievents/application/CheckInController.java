package unievents.application;

import unievents.domain.Attendance;
import unievents.domain.Event;
import unievents.domain.Registration;
import unievents.domain.Student;
import unievents.infrastructure.persistence.DataHandler;
import unievents.shared.CheckInMethod;
import unievents.shared.DomainException;
import unievents.shared.EventStatus;
import unievents.shared.InvalidCheckInCodeException;

/**
 * UC4 / SD-04: Student Self Check-In at Event using Check-In Code.
 */
public class CheckInController {
    private final DataHandler dataHandler;
    private final Student student;

    public CheckInController(DataHandler dataHandler, Student student) {
        this.dataHandler = dataHandler;
        this.student = student;
    }

    public Attendance checkInToEvent(int eventId, String checkInCode) {
        if (checkInCode == null || checkInCode.isBlank()) {
            throw new InvalidCheckInCodeException("Please enter the event check-in code.");
        }
        Event event = dataHandler.findEventById(eventId);
        if (event == null) {
            throw new DomainException("Event #" + eventId + " was not found.");
        }
        if (event.getStatus() == EventStatus.CANCELLED || event.getStatus() == EventStatus.DRAFT) {
            throw new DomainException("Check-in is unavailable because the event status is " + event.getStatus() + ".");
        }
        if (!event.verifyCheckInCode(checkInCode)) {
            throw new InvalidCheckInCodeException("Invalid check-in code for \"" + event.getTitle() + "\".");
        }

        Registration registration = event.findRegistrationFor(student);
        if (registration == null) {
            throw new DomainException("You do not have a registration for \"" + event.getTitle() + "\".");
        }

        Attendance attendance = registration.markAttended(CheckInMethod.CODE);
        dataHandler.saveRegistration(registration);
        return attendance;
    }
}
