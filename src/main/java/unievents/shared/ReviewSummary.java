package unievents.shared;

import java.time.LocalDateTime;

public record ReviewSummary(
        int reviewId,
        String studentName,
        int rating,
        String comment,
        LocalDateTime postedAt
) {
    public int getReviewId() { return reviewId; }
    public String getStudentName() { return studentName; }
    public int getRating() { return rating; }
    public String getComment() { return comment; }
    public LocalDateTime getPostedAt() { return postedAt; }
}
