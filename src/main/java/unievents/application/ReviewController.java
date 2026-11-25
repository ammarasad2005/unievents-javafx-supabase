package unievents.application;

import unievents.domain.Event;
import unievents.domain.Review;
import unievents.domain.Student;
import unievents.infrastructure.persistence.DataHandler;
import unievents.shared.DomainException;

/**
 * UC5 / SD-05: Student Event Review Eligibility Check and Rating/Review Submission.
 */
public class ReviewController {
    private final DataHandler dataHandler;
    private final Student student;

    public ReviewController(DataHandler dataHandler, Student student) {
        this.dataHandler = dataHandler;
        this.student = student;
    }

    public boolean checkReviewEligibility(int eventId) {
        Event event = dataHandler.findEventById(eventId);
        if (event == null) {
            throw new DomainException("Event #" + eventId + " was not found.");
        }
        return event.isEligibleForReview(student);
    }

    public Review submitReview(int eventId, int rating, String comment) {
        Event event = dataHandler.findEventById(eventId);
        if (event == null) {
            throw new DomainException("Event #" + eventId + " was not found.");
        }
        Review review = event.addReview(student, rating, comment);
        dataHandler.saveReview(review);
        return review;
    }
}
