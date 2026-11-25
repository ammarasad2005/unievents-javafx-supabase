package unievents.application;

import unievents.domain.Attendance;
import unievents.domain.Category;
import unievents.domain.Club;
import unievents.domain.Notification;
import unievents.domain.Registration;
import unievents.domain.Review;
import unievents.domain.Student;
import unievents.infrastructure.persistence.DataHandler;
import unievents.shared.EventDetails;
import unievents.shared.EventSummary;
import unievents.shared.RegistrationStatus;
import unievents.shared.RegistrationSummary;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Deliverable 5 Design Class Diagram StudentController facade.
 * Delegates cleanly to cohesive SD-01..SD-05 controllers so both DCD and SDs align 1-for-1.
 */
public class StudentController {
    private final Student student;
    private final DataHandler dataHandler;
    private final EventBrowseController browseController;
    private final RegistrationController registrationController;
    private final CheckInController checkInController;
    private final ReviewController reviewController;

    public StudentController(DataHandler dataHandler, Student student) {
        this.dataHandler = dataHandler;
        this.student = student;
        this.browseController = new EventBrowseController(dataHandler);
        this.registrationController = new RegistrationController(dataHandler, student);
        this.checkInController = new CheckInController(dataHandler, student);
        this.reviewController = new ReviewController(dataHandler, student);
    }

    public Student getStudent() {
        return student;
    }

    public List<EventSummary> getPublishedEvents() {
        return browseController.getPublishedEvents();
    }

    public List<EventSummary> filterEvents(Integer categoryId, Integer clubId, LocalDateTime date) {
        return browseController.filterEvents(categoryId, clubId, date);
    }

    public EventDetails getEventDetails(int eventId) {
        return browseController.getEventDetails(eventId);
    }

    public Registration registerForEvent(int eventId) {
        return registrationController.registerForEvent(eventId);
    }

    public List<RegistrationSummary> getMyRegistrations() {
        return registrationController.getMyRegistrations();
    }

    public RegistrationStatus cancelRegistration(int registrationId) {
        return registrationController.cancelRegistration(registrationId);
    }

    public Attendance checkInToEvent(int eventId, String checkInCode) {
        return checkInController.checkInToEvent(eventId, checkInCode);
    }

    public boolean checkReviewEligibility(int eventId) {
        return reviewController.checkReviewEligibility(eventId);
    }

    public Review submitReview(int eventId, int rating, String comment) {
        return reviewController.submitReview(eventId, rating, comment);
    }

    public List<Category> getCategories() {
        return browseController.getCategories();
    }

    public List<Club> getClubs() {
        return browseController.getClubs();
    }

    public List<Notification> getMyNotifications() {
        return dataHandler.findNotificationsByUserId(student.getUserId());
    }

    public void markAllNotificationsRead() {
        dataHandler.markNotificationsRead(student.getUserId());
    }

    public EventBrowseController getBrowseController() { return browseController; }
    public RegistrationController getRegistrationController() { return registrationController; }
    public CheckInController getCheckInController() { return checkInController; }
    public ReviewController getReviewController() { return reviewController; }
}
