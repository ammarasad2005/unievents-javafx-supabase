package unievents.shared;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Full event detail projection used across SD-01, SD-02, SD-06, and SD-07.
 */
public record EventDetails(
        int eventId,
        String title,
        String description,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        String venue,
        int capacity,
        int remainingSeats,
        int registeredCount,
        int waitlistedCount,
        int categoryId,
        String categoryName,
        int clubId,
        String clubName,
        EventStatus status,
        String checkInCode,
        double averageRating,
        List<ReviewSummary> reviews
) {
    public int getEventId() { return eventId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public LocalDateTime getStartDateTime() { return startDateTime; }
    public LocalDateTime getEndDateTime() { return endDateTime; }
    public String getVenue() { return venue; }
    public int getCapacity() { return capacity; }
    public int getRemainingSeats() { return remainingSeats; }
    public int getRegisteredCount() { return registeredCount; }
    public int getWaitlistedCount() { return waitlistedCount; }
    public int getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public int getClubId() { return clubId; }
    public String getClubName() { return clubName; }
    public EventStatus getStatus() { return status; }
    public String getCheckInCode() { return checkInCode; }
    public double getAverageRating() { return averageRating; }
    public List<ReviewSummary> getReviews() { return reviews; }
}
