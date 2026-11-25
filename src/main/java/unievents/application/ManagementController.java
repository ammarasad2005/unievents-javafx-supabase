package unievents.application;

import unievents.domain.Admin;
import unievents.domain.Announcement;
import unievents.domain.AttendanceReport;
import unievents.domain.Category;
import unievents.domain.Club;
import unievents.domain.ClubOrganizer;
import unievents.domain.Event;
import unievents.infrastructure.persistence.DataHandler;
import unievents.shared.AttendanceSheet;
import unievents.shared.AudienceCounts;
import unievents.shared.AudienceType;
import unievents.shared.ConflictDetails;
import unievents.shared.ConflictStatus;
import unievents.shared.ConflictSummary;
import unievents.shared.EventDetails;
import unievents.shared.EventStatus;
import unievents.shared.EventSummary;
import unievents.shared.ResolutionType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Deliverable 5 Design Class Diagram ManagementController facade.
 * Delegates cleanly to cohesive SD-06..SD-12 controllers for Organizer and Admin operations.
 */
public class ManagementController {
    private final ClubOrganizer organizer;
    private final Admin admin;
    private final EventManagementController eventManagementController;
    private final AnnouncementController announcementController;
    private final AttendanceManagementController attendanceManagementController;
    private final ClubController clubController;
    private final ConflictController conflictController;

    public ManagementController(DataHandler dataHandler, ClubOrganizer organizer, Admin admin) {
        this.organizer = organizer;
        this.admin = admin;
        this.eventManagementController = organizer != null ? new EventManagementController(dataHandler, organizer) : null;
        this.announcementController = organizer != null ? new AnnouncementController(dataHandler, organizer) : null;
        this.attendanceManagementController = organizer != null ? new AttendanceManagementController(dataHandler, organizer) : null;
        this.clubController = admin != null ? new ClubController(dataHandler, admin) : null;
        this.conflictController = admin != null ? new ConflictController(dataHandler, admin) : null;
    }

    public static ManagementController forOrganizer(DataHandler dataHandler, ClubOrganizer organizer) {
        return new ManagementController(dataHandler, organizer, null);
    }

    public static ManagementController forAdmin(DataHandler dataHandler, Admin admin) {
        return new ManagementController(dataHandler, null, admin);
    }

    public ClubOrganizer getOrganizer() { return organizer; }
    public Admin getAdmin() { return admin; }

    // --- Organizer Operations (UC6 – UC10) ---

    public List<EventSummary> getMyEvents() {
        return eventManagementController.getMyEvents();
    }

    public EventDetails getEventDetails(int eventId) {
        return eventManagementController.getEventDetails(eventId);
    }

    public List<Category> getCategories() {
        return eventManagementController.getCategories();
    }

    public Event saveEvent(Integer eventId, String title, String description,
                           LocalDateTime startDateTime, LocalDateTime endDateTime,
                           String venue, int capacity, int categoryId) {
        return eventManagementController.saveEvent(
                eventId, title, description, startDateTime, endDateTime, venue, capacity, categoryId);
    }

    public EventStatus changeEventStatus(int eventId, EventStatus newStatus, String reason) {
        return eventManagementController.changeEventStatus(eventId, newStatus, reason);
    }

    public AudienceCounts getAudienceCounts(int eventId) {
        return announcementController.getAudienceCounts(eventId);
    }

    public Announcement sendAnnouncement(int eventId, String subject, String message, AudienceType audience) {
        return announcementController.sendAnnouncement(eventId, subject, message, audience);
    }

    public AttendanceSheet getAttendanceSheet(int eventId) {
        return attendanceManagementController.getAttendanceSheet(eventId);
    }

    public int recordAttendance(int eventId, List<Integer> registrationIds) {
        return attendanceManagementController.recordAttendance(eventId, registrationIds);
    }

    public AttendanceReport generateAttendanceReport(int eventId) {
        return attendanceManagementController.generateAttendanceReport(eventId);
    }

    // --- Admin Operations (UC11 – UC12) ---

    public Club createClub(String name, String description, Integer presidentId) {
        return clubController.createClub(name, description, presidentId);
    }

    public List<ClubOrganizer> getAvailableOrganizers() {
        return clubController.getAvailableOrganizers();
    }

    public List<Club> getAllClubs() {
        return clubController.getAllClubs();
    }

    public List<ConflictSummary> getOpenConflicts() {
        return conflictController.getOpenConflicts();
    }

    public ConflictDetails getConflictDetails(int conflictId) {
        return conflictController.getConflictDetails(conflictId);
    }

    public ConflictStatus resolveConflict(int conflictId, ResolutionType resolution, String reason) {
        return conflictController.resolveConflict(conflictId, resolution, reason);
    }

    public EventManagementController getEventManagementController() { return eventManagementController; }
    public AnnouncementController getAnnouncementController() { return announcementController; }
    public AttendanceManagementController getAttendanceManagementController() { return attendanceManagementController; }
    public ClubController getClubController() { return clubController; }
    public ConflictController getConflictController() { return conflictController; }
}
