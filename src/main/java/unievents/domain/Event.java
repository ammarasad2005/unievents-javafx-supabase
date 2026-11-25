package unievents.domain;

import unievents.shared.AttendanceRow;
import unievents.shared.AudienceCounts;
import unievents.shared.AudienceType;
import unievents.shared.CheckInMethod;
import unievents.shared.DomainException;
import unievents.shared.EventDetails;
import unievents.shared.EventStatus;
import unievents.shared.EventSummary;
import unievents.shared.NotificationType;
import unievents.shared.RegistrationStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Core Information Expert & Creator for event lifecycle, seats, waitlist, reviews, attendance, and reporting.
 */
public class Event {
    private int eventId;
    private final Club club;
    private Category category;
    private String title;
    private String description;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String venue;
    private int capacity;
    private EventStatus status;
    private final String checkInCode;
    private final List<Registration> registrations = new ArrayList<>();
    private final List<Review> reviews = new ArrayList<>();

    public Event(int eventId, Club club, Category category,
                 String title, String description,
                 LocalDateTime startDateTime, LocalDateTime endDateTime,
                 String venue, int capacity,
                 EventStatus status, String checkInCode) {
        validateCoreFields(title, startDateTime, endDateTime, venue, capacity, category);
        if (club == null) {
            throw new DomainException("Hosting club is required for an event.");
        }
        if (status == null) {
            throw new DomainException("Event status is required.");
        }
        if (checkInCode == null || checkInCode.isBlank()) {
            throw new DomainException("Event check-in code is required.");
        }
        this.eventId = eventId;
        this.club = club;
        this.category = category;
        this.title = title.trim();
        this.description = description != null ? description.trim() : "";
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.venue = venue.trim();
        this.capacity = capacity;
        this.status = status;
        this.checkInCode = checkInCode.trim();
    }

    private static void validateCoreFields(String title, LocalDateTime startDateTime,
                                           LocalDateTime endDateTime, String venue,
                                           int capacity, Category category) {
        if (title == null || title.isBlank()) {
            throw new DomainException("Event title is required.");
        }
        if (venue == null || venue.isBlank()) {
            throw new DomainException("Event venue is required.");
        }
        if (category == null) {
            throw new DomainException("Event category is required.");
        }
        if (startDateTime == null || endDateTime == null) {
            throw new DomainException("Event start and end date/time are required.");
        }
        if (!endDateTime.isAfter(startDateTime)) {
            throw new DomainException("Event end date/time must be strictly after start date/time.");
        }
        if (capacity <= 0) {
            throw new DomainException("Event capacity must be greater than zero.");
        }
    }

    public void assignEventId(int id) {
        if (this.eventId == 0 && id > 0) {
            this.eventId = id;
        }
    }

    public void attachHydratedRegistration(Registration registration) {
        if (registration != null && !registrations.contains(registration)) {
            registrations.add(registration);
            registration.getStudent().addRegistration(registration);
        }
    }

    public void attachHydratedReview(Review review) {
        if (review != null && !reviews.contains(review)) {
            reviews.add(review);
        }
    }

    /**
     * Confirmed seat count (REGISTERED + ATTENDED students occupy confirmed event seats).
     */
    public int getRegisteredCount() {
        int count = 0;
        for (Registration reg : registrations) {
            if (reg.getStatus() == RegistrationStatus.REGISTERED
                    || reg.getStatus() == RegistrationStatus.ATTENDED) {
                count++;
            }
        }
        return count;
    }

    public int getWaitlistedCount() {
        int count = 0;
        for (Registration reg : registrations) {
            if (reg.getStatus() == RegistrationStatus.WAITLISTED) {
                count++;
            }
        }
        return count;
    }

    public int getAttendedCount() {
        int count = 0;
        for (Registration reg : registrations) {
            if (reg.isAttended()) {
                count++;
            }
        }
        return count;
    }

    public int getRemainingSeats() {
        return Math.max(0, capacity - getRegisteredCount());
    }

    /**
     * UC2 / SD-02 (Information Expert + Creator): Registers a student as REGISTERED if seats remain,
     * otherwise places them on the waitlist with the next sequential position.
     */
    public Registration register(Student student) {
        if (student == null) {
            throw new DomainException("Student is required to register.");
        }
        if (status != EventStatus.PUBLISHED) {
            throw new DomainException("Registration is only open for PUBLISHED events (current status: " + status + ").");
        }

        Registration existing = findRegistrationFor(student);
        if (existing != null && existing.getStatus() != RegistrationStatus.CANCELLED) {
            throw new DomainException("You already have an active registration for \""
                    + title + "\" (status: " + existing.getStatus() + ").");
        }

        RegistrationStatus assignedStatus;
        Integer waitlistPosition;
        NotificationType notifType;
        String notifMessage;

        if (getRemainingSeats() > 0) {
            assignedStatus = RegistrationStatus.REGISTERED;
            waitlistPosition = null;
            notifType = NotificationType.REGISTRATION;
            notifMessage = "Confirmed seat for \"" + title + "\" at " + venue + ".";
        } else {
            assignedStatus = RegistrationStatus.WAITLISTED;
            waitlistPosition = getWaitlist().size() + 1;
            notifType = NotificationType.WAITLIST;
            notifMessage = "Added to waitlist (#" + waitlistPosition + ") for \"" + title + "\".";
        }

        Registration registration;
        if (existing != null) {
            existing.reactivate(assignedStatus, waitlistPosition);
            registration = existing;
        } else {
            registration = new Registration(student, this, assignedStatus, waitlistPosition);
            registrations.add(registration);
            student.addRegistration(registration);
        }

        student.receiveNotification(new Notification(student, notifMessage, notifType));
        return registration;
    }

    /**
     * UC3 / SD-03: Promotes the earliest waitlisted student when a seat opens up.
     */
    public Registration promoteNextWaitlisted() {
        if (getRemainingSeats() <= 0) {
            resequenceWaitlist();
            return null;
        }
        List<Registration> waitlist = getWaitlist();
        if (waitlist.isEmpty()) {
            return null;
        }
        Registration next = waitlist.get(0);
        next.promote();
        resequenceWaitlist();

        Student promotedStudent = next.getStudent();
        promotedStudent.receiveNotification(new Notification(
                promotedStudent,
                "Good news! A seat opened up and you have been promoted to REGISTERED for \"" + title + "\".",
                NotificationType.PROMOTION
        ));
        return next;
    }

    public void resequenceWaitlist() {
        List<Registration> waitlist = getWaitlist();
        for (int i = 0; i < waitlist.size(); i++) {
            waitlist.get(i).updateWaitlistPosition(i + 1);
        }
    }

    public List<Registration> getWaitlist() {
        List<Registration> waitlisted = new ArrayList<>();
        for (Registration reg : registrations) {
            if (reg.getStatus() == RegistrationStatus.WAITLISTED) {
                waitlisted.add(reg);
            }
        }
        waitlisted.sort(Comparator
                .comparing((Registration r) -> r.getWaitlistPosition() != null ? r.getWaitlistPosition() : Integer.MAX_VALUE)
                .thenComparing(Registration::getRegisteredAt));
        return Collections.unmodifiableList(waitlisted);
    }

    public Registration findRegistrationFor(Student student) {
        if (student == null) {
            return null;
        }
        for (Registration reg : registrations) {
            if (reg.getStudent().getUserId() == student.getUserId()
                    || reg.getStudent().equals(student)) {
                return reg;
            }
        }
        return null;
    }

    public Registration findRegistration(int registrationId) {
        for (Registration reg : registrations) {
            if (reg.getRegistrationId() == registrationId) {
                return reg;
            }
        }
        return null;
    }

    public boolean verifyCheckInCode(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        return this.checkInCode.equalsIgnoreCase(code.trim());
    }

    public boolean hasReviewFrom(Student student) {
        if (student == null) {
            return false;
        }
        for (Review r : reviews) {
            if (r.getStudent().getUserId() == student.getUserId()
                    || r.getStudent().equals(student)) {
                return true;
            }
        }
        return false;
    }

    /**
     * UC5 / SD-05: Eligible only if the student attended this event and has not already reviewed it.
     */
    public boolean isEligibleForReview(Student student) {
        if (student == null) {
            return false;
        }
        Registration reg = findRegistrationFor(student);
        if (reg == null || !reg.isAttended()) {
            return false;
        }
        return !hasReviewFrom(student);
    }

    /**
     * UC5 / SD-05 (Creator): Validates review eligibility and rating, creates Review, updates average.
     */
    public Review addReview(Student student, int rating, String comment) {
        if (student == null) {
            throw new DomainException("Student is required to submit a review.");
        }
        Registration reg = findRegistrationFor(student);
        if (reg == null || !reg.isAttended()) {
            throw new DomainException("Only students who attended \"" + title + "\" can submit a review.");
        }
        if (hasReviewFrom(student)) {
            throw new DomainException("You have already submitted a review for \"" + title + "\".");
        }
        Review review = new Review(student, this, rating, comment);
        reviews.add(review);
        return review;
    }

    public double getAverageRating() {
        if (reviews.isEmpty()) {
            return 0.0;
        }
        int sum = 0;
        for (Review r : reviews) {
            sum += r.getRating();
        }
        return Math.round((sum * 10.0) / reviews.size()) / 10.0;
    }

    /**
     * UC6 / SD-06: Updates mutable event details while enforcing capacity >= registeredCount.
     */
    public void updateDetails(String title, String description,
                              LocalDateTime startDateTime, LocalDateTime endDateTime,
                              String venue, int capacity, Category category) {
        if (status == EventStatus.CANCELLED || status == EventStatus.COMPLETED) {
            throw new DomainException("Cannot edit an event whose status is " + status + ".");
        }
        validateCoreFields(title, startDateTime, endDateTime, venue, capacity, category);
        int currentRegistered = getRegisteredCount();
        if (capacity < currentRegistered) {
            throw new DomainException("Capacity (" + capacity + ") cannot be lower than current confirmed registrations ("
                    + currentRegistered + ").");
        }
        this.title = title.trim();
        this.description = description != null ? description.trim() : "";
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.venue = venue.trim();
        this.capacity = capacity;
        this.category = category;
    }

    void adjustCapacityForConflictResolution(int minimumCapacity) {
        if (minimumCapacity > this.capacity) {
            this.capacity = minimumCapacity;
        }
    }

    /**
     * UC7 / SD-07: Enforces valid state machine transitions on EventStatus.
     */
    public void validateTransition(EventStatus newStatus) {
        if (newStatus == null) {
            throw new DomainException("Target event status is required.");
        }
        if (this.status == newStatus) {
            throw new DomainException("Event is already in " + newStatus + " status.");
        }
        boolean valid = switch (this.status) {
            case DRAFT -> newStatus == EventStatus.PUBLISHED || newStatus == EventStatus.CANCELLED;
            case PUBLISHED -> newStatus == EventStatus.COMPLETED || newStatus == EventStatus.CANCELLED;
            case CANCELLED, COMPLETED -> false;
        };
        if (!valid) {
            throw new DomainException("Invalid status transition from " + this.status + " to " + newStatus + ".");
        }
    }

    public EventStatus changeStatus(EventStatus newStatus, String reason) {
        validateTransition(newStatus);
        this.status = newStatus;
        if (newStatus == EventStatus.CANCELLED) {
            String suffix = (reason != null && !reason.isBlank()) ? " Reason: " + reason.trim() : "";
            for (Student s : getAudience(AudienceType.BOTH)) {
                s.receiveNotification(new Notification(
                        s,
                        "Event \"" + title + "\" has been cancelled." + suffix,
                        NotificationType.EVENT_CANCELLED
                ));
            }
        }
        return this.status;
    }

    /**
     * UC8 / SD-08: Audience counts for organizer announcements.
     */
    public AudienceCounts getAudienceCounts() {
        int reg = getRegisteredCount();
        int wait = getWaitlistedCount();
        return new AudienceCounts(reg, wait, reg + wait);
    }

    public List<Student> getAudience(AudienceType audience) {
        if (audience == null) {
            throw new DomainException("Audience type is required.");
        }
        List<Student> recipients = new ArrayList<>();
        for (Registration reg : registrations) {
            RegistrationStatus st = reg.getStatus();
            boolean include = switch (audience) {
                case REGISTERED -> st == RegistrationStatus.REGISTERED || st == RegistrationStatus.ATTENDED;
                case WAITLISTED -> st == RegistrationStatus.WAITLISTED;
                case BOTH -> st == RegistrationStatus.REGISTERED
                        || st == RegistrationStatus.ATTENDED
                        || st == RegistrationStatus.WAITLISTED;
            };
            if (include && !recipients.contains(reg.getStudent())) {
                recipients.add(reg.getStudent());
            }
        }
        return Collections.unmodifiableList(recipients);
    }

    /**
     * UC9 / SD-09: Builds the attendance sheet rows for confirmed registrants.
     */
    public List<AttendanceRow> getAttendanceSheet() {
        List<AttendanceRow> rows = new ArrayList<>();
        for (Registration reg : registrations) {
            if (reg.getStatus() == RegistrationStatus.REGISTERED
                    || reg.getStatus() == RegistrationStatus.ATTENDED) {
                rows.add(reg.toAttendanceRow());
            }
        }
        return Collections.unmodifiableList(rows);
    }

    /**
     * UC9 / SD-09: Marks selected registrations as ATTENDED using MANUAL check-in method.
     */
    public List<Registration> recordAttendance(List<Integer> registrationIds) {
        if (status == EventStatus.DRAFT || status == EventStatus.CANCELLED) {
            throw new DomainException("Cannot record attendance for a " + status + " event.");
        }
        if (registrationIds == null || registrationIds.isEmpty()) {
            throw new DomainException("Select at least one student registration to mark attendance.");
        }
        List<Registration> updated = new ArrayList<>();
        for (Integer regId : registrationIds) {
            if (regId == null) {
                continue;
            }
            Registration reg = findRegistration(regId);
            if (reg == null) {
                throw new DomainException("Registration #" + regId + " does not belong to event \"" + title + "\".");
            }
            if (!reg.isAttended()) {
                reg.markAttended(CheckInMethod.MANUAL);
                updated.add(reg);
            }
        }
        return updated;
    }

    /**
     * UC10 / SD-10 (Creator): Aggregates registration & attendance figures into an AttendanceReport.
     */
    public AttendanceReport generateReport(ClubOrganizer organizer) {
        if (organizer == null) {
            throw new DomainException("Organizer is required to generate an attendance report.");
        }
        return new AttendanceReport(organizer, this, getRegisteredCount(), getAttendedCount());
    }

    public EventSummary toSummary() {
        return SummaryFactory.createEventSummary(this);
    }

    public EventSummary toManagementSummary() {
        return SummaryFactory.createEventSummary(this);
    }

    public EventDetails toDetails() {
        return SummaryFactory.createEventDetails(this);
    }

    public EventDetails toManagementDetails() {
        return SummaryFactory.createEventDetails(this);
    }

    public int getEventId() { return eventId; }
    public Club getClub() { return club; }
    public Category getCategory() { return category; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public LocalDateTime getStartDateTime() { return startDateTime; }
    public LocalDateTime getEndDateTime() { return endDateTime; }
    public String getVenue() { return venue; }
    public int getCapacity() { return capacity; }
    public EventStatus getStatus() { return status; }
    public String getCheckInCode() { return checkInCode; }
    public List<Registration> getRegistrations() { return Collections.unmodifiableList(registrations); }
    public List<Review> getReviews() { return Collections.unmodifiableList(reviews); }
}
