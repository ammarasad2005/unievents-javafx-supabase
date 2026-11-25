package unievents.application;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import unievents.domain.Admin;
import unievents.domain.Announcement;
import unievents.domain.Attendance;
import unievents.domain.AttendanceReport;
import unievents.domain.Club;
import unievents.domain.ClubOrganizer;
import unievents.domain.Event;
import unievents.domain.Registration;
import unievents.domain.Review;
import unievents.domain.Student;
import unievents.domain.User;
import unievents.infrastructure.persistence.DataHandler;
import unievents.infrastructure.persistence.DatabaseController;
import unievents.infrastructure.persistence.SupabaseHandler;
import unievents.shared.AttendanceSheet;
import unievents.shared.AudienceCounts;
import unievents.shared.AudienceType;
import unievents.shared.CheckInMethod;
import unievents.shared.ConflictDetails;
import unievents.shared.ConflictStatus;
import unievents.shared.ConflictSummary;
import unievents.shared.DomainException;
import unievents.shared.EventDetails;
import unievents.shared.EventStatus;
import unievents.shared.EventSummary;
import unievents.shared.InvalidCheckInCodeException;
import unievents.shared.RegistrationStatus;
import unievents.shared.RegistrationSummary;
import unievents.shared.ResolutionType;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end PostgreSQL JDBC integration tests covering all 12 Deliverable 5 Use Cases
 * through AuthController, StudentController, ManagementController, SupabaseHandler, and DatabaseController.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PostgresIntegrationTest {

    private static DataHandler dataHandler;

    @BeforeAll
    static void initDb() {
        DatabaseController db = DatabaseController.getInstance();
        Assumptions.assumeFalse(db.hasPlaceholderCredentials(),
                "Skipping live PostgreSQL JDBC test because db.password is still a placeholder.");
        dataHandler = new SupabaseHandler(db);
    }

    @Test
    @Order(1)
    void uc01_browseFilterAndInspectPublishedEvents() {
        AuthController auth = new AuthController(dataHandler);
        User loggedIn = auth.login("ali.raza@nuces.edu", "demo123");
        assertEquals("STUDENT", loggedIn.getRole());

        Student ali = dataHandler.findStudentById(loggedIn.getUserId());
        StudentController sc = new StudentController(dataHandler, ali);

        List<EventSummary> published = sc.getPublishedEvents();
        assertTrue(published.size() >= 4, "Expected at least 4 published events from seed");
        // DRAFT and COMPLETED events must never appear in getPublishedEvents
        assertTrue(published.stream().allMatch(e -> e.status() == EventStatus.PUBLISHED));

        // Filter by Sports category (id = 2)
        List<EventSummary> sportsOnly = sc.filterEvents(2, null, null);
        assertEquals(1, sportsOnly.size());
        assertEquals("Marathon 5K", sportsOnly.get(0).title());

        // Event details check
        EventDetails details = sc.getEventDetails(4);
        assertEquals(150, details.remainingSeats());
    }

    @Test
    @Order(2)
    void uc02_and_uc03_registerConfirmedWaitlistAndCancelWithAutoPromotion() {
        Student ali = dataHandler.findStudentById(5);
        Student fatima = dataHandler.findStudentById(6);
        Student zainab = dataHandler.findStudentById(8);

        StudentController aliCtrl = new StudentController(dataHandler, ali);
        StudentController fatimaCtrl = new StudentController(dataHandler, fatima);
        StudentController zainabCtrl = new StudentController(dataHandler, zainab);

        // UC2 (a): Ali registers for Marathon 5K (Event 4, seats open) -> REGISTERED
        Registration marReg = aliCtrl.registerForEvent(4);
        assertEquals(RegistrationStatus.REGISTERED, marReg.getStatus());

        // UC2 (b): Ali registers for Cloud Native Docker Workshop (Event 6, full 2/2) -> WAITLISTED #2
        Registration dockerWait = aliCtrl.registerForEvent(6);
        assertEquals(RegistrationStatus.WAITLISTED, dockerWait.getStatus());
        assertEquals(2, dockerWait.getWaitlistPosition());

        // UC3: Fatima cancels her confirmed seat (reg #13) on Event 6 -> Zainab (#1) auto-promotes to REGISTERED, Ali becomes #1!
        RegistrationSummary fatimaDockerReg = fatimaCtrl.getMyRegistrations().stream()
                .filter(r -> r.eventId() == 6)
                .findFirst().orElseThrow();
        RegistrationStatus cancelSt = fatimaCtrl.cancelRegistration(fatimaDockerReg.registrationId());
        assertEquals(RegistrationStatus.CANCELLED, cancelSt);

        RegistrationSummary zainabAfter = zainabCtrl.getMyRegistrations().stream()
                .filter(r -> r.eventId() == 6)
                .findFirst().orElseThrow();
        assertEquals(RegistrationStatus.REGISTERED, zainabAfter.status());
        assertNull(zainabAfter.waitlistPosition());

        RegistrationSummary aliAfter = aliCtrl.getMyRegistrations().stream()
                .filter(r -> r.eventId() == 6)
                .findFirst().orElseThrow();
        assertEquals(RegistrationStatus.WAITLISTED, aliAfter.status());
        assertEquals(1, aliAfter.waitlistPosition());
    }

    @Test
    @Order(3)
    void uc04_and_uc05_checkInWithCodeAndRateReviewEvent() {
        Student ali = dataHandler.findStudentById(5);
        StudentController aliCtrl = new StudentController(dataHandler, ali);

        // UC4: Invalid code throws InvalidCheckInCodeException
        assertThrows(InvalidCheckInCodeException.class,
                () -> aliCtrl.checkInToEvent(2, "WRONG-CODE"));

        // UC4: Valid code checks Ali into Tech Dev Hour (Event 2) -> ATTENDED
        Attendance att = aliCtrl.checkInToEvent(2, "ACM-DEVHOUR-26");
        assertEquals(CheckInMethod.CODE, att.getMethod());
        assertTrue(att.getAttendanceId() > 0);

        // UC5: Ali attended Event 3 (Inter-University Hackathon) and hasn't reviewed yet -> eligible
        assertTrue(aliCtrl.checkReviewEligibility(3));
        Review review = aliCtrl.submitReview(3, 4, "Challenging problems and great organization!");
        assertTrue(review.getReviewId() > 0);
        assertFalse(aliCtrl.checkReviewEligibility(3));

        // Average rating on Event 3 updated from 5.0 to (5 + 4)/2 = 4.5
        EventDetails ev3 = aliCtrl.getEventDetails(3);
        assertEquals(4.5, ev3.averageRating(), 0.01);
    }

    @Test
    @Order(4)
    void uc06_to_uc10_organizerEventLifecycleAnnouncementsAttendanceAndReport() {
        ClubOrganizer hamza = dataHandler.findOrganizerById(2);
        ManagementController mc = ManagementController.forOrganizer(dataHandler, hamza);

        // UC6: Create new DRAFT event
        Event created = mc.saveEvent(
                null,
                "System Design Masterclass",
                "Deep dive into distributed systems and GRASP patterns.",
                LocalDateTime.of(2026, 12, 12, 14, 0),
                LocalDateTime.of(2026, 12, 12, 17, 0),
                "CS Auditorium",
                40,
                1
        );
        assertTrue(created.getEventId() > 0);
        assertEquals(EventStatus.DRAFT, created.getStatus());

        // UC6: Cannot reduce Event 2 capacity below its 4 confirmed registrations
        assertThrows(DomainException.class, () -> mc.saveEvent(
                2,
                "Tech Dev Hour",
                "Updated description",
                LocalDateTime.of(2026, 11, 26, 14, 0),
                LocalDateTime.of(2026, 11, 26, 16, 0),
                "CS Lab 3",
                2,
                1
        ));

        // UC7: Publish Event 5 (Freshers Welcome Night: DRAFT -> PUBLISHED)
        EventStatus publishedStatus = mc.changeEventStatus(5, EventStatus.PUBLISHED, null);
        assertEquals(EventStatus.PUBLISHED, publishedStatus);

        // UC8: Audience counts & send announcement on Event 6
        AudienceCounts counts = mc.getAudienceCounts(6);
        assertEquals(2, counts.registeredCount());
        assertEquals(1, counts.waitlistedCount());
        assertEquals(3, counts.bothCount());

        Announcement ann = mc.sendAnnouncement(6, "Pre-requisite Setup", "Please install Docker Desktop beforehand.", AudienceType.BOTH);
        assertTrue(ann.getAnnouncementId() > 0);
        assertEquals(3, ann.getNotifiedCount());

        // UC9: Mark manual attendance on Event 2 for Fatima (reg #7) and Bilal (reg #8)
        AttendanceSheet sheet = mc.getAttendanceSheet(2);
        assertEquals("ACM-DEVHOUR-26", sheet.checkInCode());
        int markedCount = mc.recordAttendance(2, List.of(7, 8));
        assertEquals(2, markedCount);

        // UC10: Generate Attendance Report on Event 2 (4 registered, 3 attended: Ali via CODE + Fatima & Bilal via MANUAL)
        AttendanceReport report = mc.generateAttendanceReport(2);
        assertTrue(report.getReportId() > 0);
        assertEquals(4, report.getRegisteredCount());
        assertEquals(3, report.getAttendedCount());
        assertEquals(4, report.getParticipants().size());
    }

    @Test
    @Order(5)
    void uc11_and_uc12_adminCreatesClubAndResolvesConflict() {
        Admin ayesha = dataHandler.findAdminById(1);
        ManagementController adminCtrl = ManagementController.forAdmin(dataHandler, ayesha);

        // UC11: Create Club and assign Usman Iqbal (user_id = 4) as President
        Club robotics = adminCtrl.createClub(
                "Robotics & Automation Society",
                "Autonomous systems, embedded hardware, and drone competitions.",
                4
        );
        assertTrue(robotics.getClubId() > 0);
        ClubOrganizer usmanReloaded = dataHandler.findOrganizerById(4);
        assertTrue(usmanReloaded.isPresident());
        assertEquals("Robotics & Automation Society", usmanReloaded.getClub().getName());

        // Duplicate club name rejected cleanly
        assertThrows(DomainException.class, () -> adminCtrl.createClub("ACM Society", "Duplicate", null));

        // UC12: Inspect & resolve open Conflict #1 (CAPACITY_BREACH on Event 1)
        List<ConflictSummary> openConflicts = adminCtrl.getOpenConflicts();
        assertEquals(1, openConflicts.size());
        assertEquals(1, openConflicts.get(0).conflictId());

        ConflictDetails details = adminCtrl.getConflictDetails(1);
        assertEquals(2, details.capacity());
        assertEquals(3, details.registeredCount());

        ConflictStatus resolved = adminCtrl.resolveConflict(
                1,
                ResolutionType.MOVE_TO_WAITLIST,
                "Moved excess bulk-imported registration to end of waitlist."
        );
        assertEquals(ConflictStatus.RESOLVED, resolved);
        assertTrue(adminCtrl.getOpenConflicts().isEmpty());
    }
}
