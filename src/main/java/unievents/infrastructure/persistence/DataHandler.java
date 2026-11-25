package unievents.infrastructure.persistence;

import unievents.domain.Admin;
import unievents.domain.Announcement;
import unievents.domain.AttendanceReport;
import unievents.domain.Category;
import unievents.domain.Club;
import unievents.domain.ClubOrganizer;
import unievents.domain.Event;
import unievents.domain.Notification;
import unievents.domain.Registration;
import unievents.domain.RegistrationConflict;
import unievents.domain.Review;
import unievents.domain.Student;
import unievents.domain.User;

import java.time.LocalDateTime;
import java.util.List;

/**
 * GRASP Protected Variation persistence contract.
 * Application controllers depend exclusively on this interface, never on JDBC or SQL details.
 */
public interface DataHandler {
    // --- Core Deliverable 5 Design Class Diagram operations ---
    Event findEventById(int eventId);
    List<Event> findPublishedEvents();
    List<Event> findEventsByFilters(Integer categoryId, Integer clubId, LocalDateTime date);
    void saveEvent(Event event);

    Category findCategoryById(int categoryId);
    void saveRegistration(Registration registration);
    void saveRegistrations(List<Registration> registrations);
    void saveReview(Review review);
    void saveAnnouncement(Announcement announcement);
    void saveAttendanceReport(AttendanceReport report);
    void saveClub(Club club);
    ClubOrganizer findOrganizerById(int userId);

    List<RegistrationConflict> findOpenConflicts();
    RegistrationConflict findConflictById(int conflictId);
    void saveConflict(RegistrationConflict conflict);

    // --- Supporting queries for the 12 Use Cases & thin session precondition ---
    User findUserByEmail(String email);
    List<User> findAllActiveUsers();
    Student findStudentById(int userId);
    Admin findAdminById(int userId);
    List<Category> findAllCategories();
    List<Club> findAllClubs();
    List<ClubOrganizer> findAllOrganizers();
    List<Notification> findNotificationsByUserId(int userId);
    void saveNotification(Notification notification);
    void markNotificationsRead(int userId);
}
