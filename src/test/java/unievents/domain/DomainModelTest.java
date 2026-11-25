package unievents.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import unievents.shared.AudienceCounts;
import unievents.shared.AudienceType;
import unievents.shared.CheckInMethod;
import unievents.shared.ConflictStatus;
import unievents.shared.ConflictType;
import unievents.shared.DomainException;
import unievents.shared.EventStatus;
import unievents.shared.NotificationType;
import unievents.shared.Participant;
import unievents.shared.PasswordUtil;
import unievents.shared.RegistrationStatus;
import unievents.shared.ResolutionType;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DomainModelTest {

    private Admin admin;
    private ClubOrganizer organizer;
    private Club club;
    private Category techCategory;
    private Student s1;
    private Student s2;
    private Student s3;
    private Student s4;

    @BeforeEach
    void setUp() {
        String hash = PasswordUtil.sha256("demo123");
        admin = new Admin(1, "Dr. Ayesha Khan", "ayesha@unievents.edu", hash, true, "Student Affairs");
        organizer = new ClubOrganizer(2, "Hamza Tariq", "hamza@nuces.edu", hash, true, false);
        club = admin.createClub("ACM Society", "Tech society", organizer);
        club.assignClubId(1);
        techCategory = new Category(1, "Technology");

        s1 = new Student(5, "Ali Raza", "ali@nuces.edu", hash, true, "23I-0001");
        s2 = new Student(6, "Fatima Noor", "fatima@nuces.edu", hash, true, "23I-0002");
        s3 = new Student(7, "Bilal Ahmed", "bilal@nuces.edu", hash, true, "24I-0003");
        s4 = new Student(8, "Zainab Sheikh", "zainab@nuces.edu", hash, true, "24I-0004");
    }

    private Event createPublishedEvent(int capacity) {
        Event event = club.createEvent(
                "AI Bootcamp",
                "Hands-on ML workshop",
                LocalDateTime.of(2026, 12, 10, 10, 0),
                LocalDateTime.of(2026, 12, 10, 14, 0),
                "Seminar Hall A",
                capacity,
                techCategory
        );
        event.assignEventId(100);
        event.changeStatus(EventStatus.PUBLISHED, null);
        return event;
    }

    @Test
    void userHierarchySupportsPolymorphismWithoutInstanceof() {
        List<User> users = List.of(s1, organizer, admin);
        List<String> roles = users.stream().map(User::getRole).toList();
        assertEquals(List.of("STUDENT", "CLUB_ORGANIZER", "ADMIN"), roles);

        UserRoleVisitor<String> badgeVisitor = new UserRoleVisitor<>() {
            @Override
            public String visitStudent(Student student) {
                return "S:" + student.getRollNumber();
            }

            @Override
            public String visitOrganizer(ClubOrganizer org) {
                return "O:" + org.getClub().getName();
            }

            @Override
            public String visitAdmin(Admin adm) {
                return "A:" + adm.getDepartment();
            }
        };

        assertEquals("S:23I-0001", users.get(0).accept(badgeVisitor));
        assertEquals("O:ACM Society", users.get(1).accept(badgeVisitor));
        assertEquals("A:Student Affairs", users.get(2).accept(badgeVisitor));
        assertTrue(organizer.isPresident());
    }

    @Test
    void registerWaitlistCancelAndAutoPromoteFlow() {
        Event event = createPublishedEvent(2);

        Registration r1 = event.register(s1);
        Registration r2 = event.register(s2);
        Registration r3 = event.register(s3);
        Registration r4 = event.register(s4);
        r1.assignRegistrationId(1);
        r2.assignRegistrationId(2);
        r3.assignRegistrationId(3);
        r4.assignRegistrationId(4);

        assertEquals(RegistrationStatus.REGISTERED, r1.getStatus());
        assertNull(r1.getWaitlistPosition());
        assertEquals(RegistrationStatus.REGISTERED, r2.getStatus());
        assertEquals(0, event.getRemainingSeats());

        assertEquals(RegistrationStatus.WAITLISTED, r3.getStatus());
        assertEquals(1, r3.getWaitlistPosition());
        assertEquals(RegistrationStatus.WAITLISTED, r4.getStatus());
        assertEquals(2, r4.getWaitlistPosition());

        // Duplicate registration must fail
        assertThrows(DomainException.class, () -> event.register(s1));

        // Cancel confirmed registration r1 -> r3 (waitlist #1) auto-promotes to REGISTERED, r4 shifts to #1
        RegistrationStatus cancelled = r1.cancel();
        assertEquals(RegistrationStatus.CANCELLED, cancelled);
        assertSame(r3, r1.getPromotedByThisCancellation());
        assertEquals(RegistrationStatus.REGISTERED, r3.getStatus());
        assertNull(r3.getWaitlistPosition());
        assertEquals(RegistrationStatus.WAITLISTED, r4.getStatus());
        assertEquals(1, r4.getWaitlistPosition());
        assertEquals(0, event.getRemainingSeats());

        // Promoted student received PROMOTION notification
        assertTrue(s3.getNotifications().stream()
                .anyMatch(n -> n.getType() == NotificationType.PROMOTION));
    }

    @Test
    void checkInAndReviewEligibilityRules() {
        Event event = createPublishedEvent(5);
        Registration r1 = event.register(s1);
        Registration r2 = event.register(s2);
        r1.assignRegistrationId(11);
        r2.assignRegistrationId(12);

        // Before check-in, s1 is NOT eligible to review
        assertFalse(event.isEligibleForReview(s1));
        assertThrows(DomainException.class, () -> event.addReview(s1, 5, "Premature review"));

        // Verify check-in code
        assertFalse(event.verifyCheckInCode("WRONG-CODE"));
        assertTrue(event.verifyCheckInCode(event.getCheckInCode().toLowerCase()));

        Attendance att1 = r1.markAttended(CheckInMethod.CODE);
        assertEquals(CheckInMethod.CODE, att1.getMethod());
        assertEquals(RegistrationStatus.ATTENDED, r1.getStatus());
        assertTrue(event.isEligibleForReview(s1));

        // Manual batch attendance for r2
        List<Registration> manuallyMarked = event.recordAttendance(List.of(12));
        assertEquals(1, manuallyMarked.size());
        assertEquals(CheckInMethod.MANUAL, r2.getAttendance().getMethod());

        // Reviews & average calculation
        event.addReview(s1, 5, "Outstanding workshop!");
        assertFalse(event.isEligibleForReview(s1)); // duplicate review blocked
        assertThrows(DomainException.class, () -> event.addReview(s1, 4, "Second review"));

        event.addReview(s2, 4, "Very informative");
        assertEquals(4.5, event.getAverageRating(), 0.01);

        // Attendance report
        AttendanceReport report = event.generateReport(organizer);
        assertEquals(2, report.getRegisteredCount());
        assertEquals(2, report.getAttendedCount());
        List<Participant> participants = report.getParticipants();
        assertEquals(2, participants.size());
    }

    @Test
    void eventCapacityGuardStatusTransitionsAndAnnouncements() {
        Event draft = club.createEvent(
                "Hackathon",
                "24h coding",
                LocalDateTime.of(2026, 12, 15, 9, 0),
                LocalDateTime.of(2026, 12, 16, 9, 0),
                "Main Hall",
                3,
                techCategory
        );
        assertEquals(EventStatus.DRAFT, draft.getStatus());
        // Cannot jump directly from DRAFT to COMPLETED
        assertThrows(DomainException.class, () -> draft.changeStatus(EventStatus.COMPLETED, null));

        draft.changeStatus(EventStatus.PUBLISHED, null);
        draft.register(s1);
        draft.register(s2);
        draft.register(s3);
        draft.register(s4); // waitlisted #1

        // Capacity cannot shrink below 3 registered students
        assertThrows(DomainException.class, () -> draft.updateDetails(
                "Hackathon", "24h coding",
                LocalDateTime.of(2026, 12, 15, 9, 0),
                LocalDateTime.of(2026, 12, 16, 9, 0),
                "Main Hall", 2, techCategory
        ));

        // Audience counts & announcement notifications
        AudienceCounts counts = draft.getAudienceCounts();
        assertEquals(3, counts.registeredCount());
        assertEquals(1, counts.waitlistedCount());
        assertEquals(4, counts.bothCount());

        Announcement ann = organizer.createAnnouncement(draft, "Bring Laptops", "Doors open at 8:30 AM", AudienceType.BOTH);
        int notified = ann.notifyRecipients(draft.getAudience(AudienceType.BOTH));
        assertEquals(4, notified);
    }

    @Test
    void conflictResolutionUpdatesStatusAndRegistration() {
        Event event = createPublishedEvent(1);
        Registration r1 = event.register(s1);
        // Simulate legacy/bulk-imported overbooked registration
        Registration overbooked = new Registration(99, s2, event, LocalDateTime.now(), RegistrationStatus.REGISTERED, null, null);
        event.attachHydratedRegistration(overbooked);

        RegistrationConflict conflict = new RegistrationConflict(
                1, event, ConflictType.CAPACITY_BREACH, ConflictStatus.OPEN,
                LocalDateTime.now(), null, null, null, null, List.of(overbooked)
        );

        ConflictStatus result = conflict.resolve(admin, ResolutionType.MOVE_TO_WAITLIST, "Moved excess bulk registration to waitlist");
        assertEquals(ConflictStatus.RESOLVED, result);
        assertNotNull(conflict.getResolvedAt());
        assertEquals(RegistrationStatus.WAITLISTED, overbooked.getStatus());
        assertEquals(1, overbooked.getWaitlistPosition());
        assertEquals(RegistrationStatus.REGISTERED, r1.getStatus());
    }
}
