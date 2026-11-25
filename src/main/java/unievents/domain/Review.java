package unievents.domain;

import unievents.shared.DomainException;
import unievents.shared.ReviewSummary;

import java.time.LocalDateTime;

public class Review {
    private int reviewId;
    private final Student student;
    private final Event event;
    private final int rating;
    private final String comment;
    private final LocalDateTime postedAt;

    public Review(Student student, Event event, int rating, String comment) {
        this(0, student, event, rating, comment, LocalDateTime.now());
    }

    public Review(int reviewId, Student student, Event event, int rating, String comment, LocalDateTime postedAt) {
        if (student == null) {
            throw new DomainException("Reviewer student is required.");
        }
        if (event == null) {
            throw new DomainException("Reviewed event is required.");
        }
        if (rating < 1 || rating > 5) {
            throw new DomainException("Rating must be between 1 and 5.");
        }
        this.reviewId = reviewId;
        this.student = student;
        this.event = event;
        this.rating = rating;
        this.comment = comment != null ? comment.trim() : "";
        this.postedAt = postedAt != null ? postedAt : LocalDateTime.now();
    }

    public void assignReviewId(int id) {
        if (this.reviewId == 0 && id > 0) {
            this.reviewId = id;
        }
    }

    public ReviewSummary toSummary() {
        return new ReviewSummary(reviewId, student.getFullName(), rating, comment, postedAt);
    }

    public int getReviewId() { return reviewId; }
    public Student getStudent() { return student; }
    public Event getEvent() { return event; }
    public int getRating() { return rating; }
    public String getComment() { return comment; }
    public LocalDateTime getPostedAt() { return postedAt; }
}
