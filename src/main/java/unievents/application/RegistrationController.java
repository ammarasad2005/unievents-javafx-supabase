package unievents.application;

import unievents.domain.Event;
import unievents.domain.Notification;
import unievents.domain.Registration;
import unievents.domain.Student;
import unievents.infrastructure.persistence.DataHandler;
import unievents.infrastructure.persistence.DatabaseController;
import unievents.shared.DomainException;
import unievents.shared.EventDetails;
import unievents.shared.RegistrationStatus;
import unievents.shared.RegistrationSummary;

import java.util.List;

/**
 * UC2 & UC3 / SD-02 & SD-03: Student Event Registration, My Registrations, and Atomic Cancellation/Promotion.
 */
public class RegistrationController {
    private final DataHandler dataHandler;
    private final Student student;

    public RegistrationController(DataHandler dataHandler, Student student) {
        this.dataHandler = dataHandler;
        this.student = student;
    }

    public EventDetails getEventDetails(int eventId) {
        Event event = dataHandler.findEventById(eventId);
        if (event == null) {
            throw new DomainException("Event #" + eventId + " was not found.");
        }
        return event.toDetails();
    }

    public Registration registerForEvent(int eventId) {
        Event event = dataHandler.findEventById(eventId);
        if (event == null) {
            throw new DomainException("Selected event no longer exists.");
        }
        Student freshStudent = dataHandler.findStudentById(student.getUserId());
        if (freshStudent == null) {
            throw new DomainException("Active student session is invalid.");
        }

        int beforeNotifs = freshStudent.getNotifications().size();
        Registration registration = event.register(freshStudent);

        DatabaseController db = DatabaseController.getInstance();
        db.beginTransaction();
        try {
            dataHandler.saveRegistration(registration);
            List<Notification> notifs = freshStudent.getNotifications();
            if (notifs.size() > beforeNotifs) {
                dataHandler.saveNotification(notifs.get(notifs.size() - 1));
            }
            db.commit();
            return registration;
        } catch (RuntimeException e) {
            db.rollback();
            throw e;
        }
    }

    public List<RegistrationSummary> getMyRegistrations() {
        Student freshStudent = dataHandler.findStudentById(student.getUserId());
        if (freshStudent == null) {
            return List.of();
        }
        return freshStudent.getRegistrations().stream()
                .map(Registration::toSummary)
                .toList();
    }

    public RegistrationStatus cancelRegistration(int registrationId) {
        Student freshStudent = dataHandler.findStudentById(student.getUserId());
        if (freshStudent == null) {
            throw new DomainException("Active student session is invalid.");
        }
        Registration registration = freshStudent.findRegistration(registrationId);
        if (registration == null) {
            throw new DomainException("Registration #" + registrationId + " was not found under your account.");
        }

        RegistrationStatus status = registration.cancel();
        Registration promoted = registration.getPromotedByThisCancellation();

        DatabaseController db = DatabaseController.getInstance();
        db.beginTransaction();
        try {
            dataHandler.saveRegistrations(registration.getEvent().getRegistrations());
            if (promoted != null) {
                List<Notification> promotedNotifs = promoted.getStudent().getNotifications();
                if (!promotedNotifs.isEmpty()) {
                    dataHandler.saveNotification(promotedNotifs.get(promotedNotifs.size() - 1));
                }
            }
            db.commit();
            return status;
        } catch (RuntimeException e) {
            db.rollback();
            throw e;
        }
    }
}
