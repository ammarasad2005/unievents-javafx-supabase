package unievents.domain;

import unievents.shared.ConflictDetails;
import unievents.shared.ConflictSummary;
import unievents.shared.EventDetails;
import unievents.shared.EventSummary;
import unievents.shared.RegistrationSummary;
import unievents.shared.ReviewSummary;

import java.util.List;

/**
 * GoF Simple Factory for assembling immutable cross-layer summary/detail DTOs from Domain entities.
 */
public final class SummaryFactory {
    private SummaryFactory() {
    }

    public static EventSummary createEventSummary(Event event) {
        return new EventSummary(
                event.getEventId(),
                event.getTitle(),
                event.getClub().getName(),
                event.getCategory().getName(),
                event.getStartDateTime(),
                event.getEndDateTime(),
                event.getVenue(),
                event.getCapacity(),
                event.getRemainingSeats(),
                event.getRegisteredCount(),
                event.getWaitlistedCount(),
                event.getStatus(),
                event.getAverageRating()
        );
    }

    public static EventDetails createEventDetails(Event event) {
        List<ReviewSummary> reviewSummaries = event.getReviews().stream()
                .map(Review::toSummary)
                .toList();
        return new EventDetails(
                event.getEventId(),
                event.getTitle(),
                event.getDescription(),
                event.getStartDateTime(),
                event.getEndDateTime(),
                event.getVenue(),
                event.getCapacity(),
                event.getRemainingSeats(),
                event.getRegisteredCount(),
                event.getWaitlistedCount(),
                event.getCategory().getCategoryId(),
                event.getCategory().getName(),
                event.getClub().getClubId(),
                event.getClub().getName(),
                event.getStatus(),
                event.getCheckInCode(),
                event.getAverageRating(),
                reviewSummaries
        );
    }

    public static RegistrationSummary createRegistrationSummary(Registration reg) {
        Event ev = reg.getEvent();
        Student st = reg.getStudent();
        return new RegistrationSummary(
                reg.getRegistrationId(),
                ev.getEventId(),
                ev.getTitle(),
                ev.getClub().getName(),
                st.getUserId(),
                st.getFullName(),
                st.getRollNumber(),
                ev.getStartDateTime(),
                ev.getEndDateTime(),
                ev.getVenue(),
                reg.getStatus(),
                reg.getWaitlistPosition(),
                reg.getRegisteredAt(),
                ev.getStatus()
        );
    }

    public static ConflictSummary createConflictSummary(RegistrationConflict conflict) {
        return new ConflictSummary(
                conflict.getConflictId(),
                conflict.getEvent().getEventId(),
                conflict.getEventTitle(),
                conflict.getStudentName(),
                conflict.getType(),
                conflict.getStatus(),
                conflict.getDetectedAt()
        );
    }

    public static ConflictDetails createConflictDetails(RegistrationConflict conflict) {
        Event event = conflict.getEvent();
        List<RegistrationSummary> concerned = conflict.getRegistrations().stream()
                .map(Registration::toSummary)
                .toList();
        List<RegistrationSummary> allHistory = event.getRegistrations().stream()
                .map(Registration::toSummary)
                .toList();
        List<RegistrationSummary> waitlist = event.getWaitlist().stream()
                .map(Registration::toSummary)
                .toList();

        return new ConflictDetails(
                conflict.getConflictId(),
                event.getEventId(),
                event.getTitle(),
                conflict.getType(),
                conflict.getStatus(),
                conflict.getDetectedAt(),
                event.getCapacity(),
                event.getRegisteredCount(),
                concerned,
                allHistory,
                waitlist,
                conflict.getResolution(),
                conflict.getReason(),
                conflict.getResolvedAt()
        );
    }
}
