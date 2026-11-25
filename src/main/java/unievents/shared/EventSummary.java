package unievents.shared;

import java.time.LocalDateTime;

/**
 * Summary DTO returned by Browse Events (SD-01) and Manage Events (SD-06, SD-07).
 */
public record EventSummary(
        int eventId,
        String title,
        String clubName,
        String categoryName,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        String venue,
        int capacity,
        int remainingSeats,
        int registeredCount,
        int waitlistedCount,
        EventStatus status,
        double averageRating
) {
    public int getEventId() { return eventId; }
    public String getTitle() { return title; }
    public String getClubName() { return clubName; }
    public String getCategoryName() { return categoryName; }
    public LocalDateTime getStartDateTime() { return startDateTime; }
    public LocalDateTime getEndDateTime() { return endDateTime; }
    public String getVenue() { return venue; }
    public int getCapacity() { return capacity; }
    public int getRemainingSeats() { return remainingSeats; }
    public int getRegisteredCount() { return registeredCount; }
    public int getWaitlistedCount() { return waitlistedCount; }
    public EventStatus getStatus() { return status; }
    public double getAverageRating() { return averageRating; }

    @Override
    public String toString() {
        return title + " (" + status + " · " + clubName + ")";
    }
}
