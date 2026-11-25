package unievents.domain;

import unievents.shared.AttendanceRow;
import unievents.shared.CheckInMethod;
import unievents.shared.DomainException;
import unievents.shared.EventStatus;
import unievents.shared.Participant;
import unievents.shared.RegistrationStatus;
import unievents.shared.RegistrationSummary;
import unievents.shared.ResolutionType;

import java.time.LocalDateTime;

public class Registration {
    private int registrationId;
    private final Student student;
    private final Event event;
    private LocalDateTime registeredAt;
    private RegistrationStatus status;
    private Integer waitlistPosition;
    private Attendance attendance;
    private Registration promotedByThisCancellation;

    public Registration(Student student, Event event, RegistrationStatus status, Integer waitlistPosition) {
        this(0, student, event, LocalDateTime.now(), status, waitlistPosition, null);
    }

    public Registration(int registrationId, Student student, Event event,
                        LocalDateTime registeredAt, RegistrationStatus status,
                        Integer waitlistPosition, Attendance attendance) {
        if (student == null) {
            throw new DomainException("Student is required for registration.");
        }
        if (event == null) {
            throw new DomainException("Event is required for registration.");
        }
        if (status == null) {
            throw new DomainException("Registration status is required.");
        }
        validateWaitlistInvariant(status, waitlistPosition);
        this.registrationId = registrationId;
        this.student = student;
        this.event = event;
        this.registeredAt = registeredAt != null ? registeredAt : LocalDateTime.now();
        this.status = status;
        this.waitlistPosition = waitlistPosition;
        this.attendance = attendance;
    }

    private static void validateWaitlistInvariant(RegistrationStatus status, Integer waitlistPosition) {
        if (status == RegistrationStatus.WAITLISTED) {
            if (waitlistPosition == null || waitlistPosition <= 0) {
                throw new DomainException("Waitlisted registration requires a positive waitlist position.");
            }
        } else if (waitlistPosition != null) {
            throw new DomainException("Only WAITLISTED registrations may hold a waitlist position.");
        }
    }

    public void assignRegistrationId(int id) {
        if (this.registrationId == 0 && id > 0) {
            this.registrationId = id;
        }
    }

    public void attachAttendance(Attendance hydratedAttendance) {
        this.attendance = hydratedAttendance;
    }

    void reactivate(RegistrationStatus newStatus, Integer newWaitlistPos) {
        validateWaitlistInvariant(newStatus, newWaitlistPos);
        this.status = newStatus;
        this.waitlistPosition = newWaitlistPos;
        this.registeredAt = LocalDateTime.now();
        this.attendance = null;
        this.promotedByThisCancellation = null;
    }

    void updateWaitlistPosition(int newPosition) {
        if (this.status == RegistrationStatus.WAITLISTED && newPosition > 0) {
            this.waitlistPosition = newPosition;
        }
    }

    void promote() {
        this.status = RegistrationStatus.REGISTERED;
        this.waitlistPosition = null;
    }

    /**
     * UC3 / SD-03: Cancels this registration. If it held a confirmed seat (REGISTERED),
     * asks Event to promote the next waitlisted student automatically.
     */
    public RegistrationStatus cancel() {
        if (status == RegistrationStatus.CANCELLED) {
            throw new DomainException("Registration is already cancelled.");
        }
        if (status == RegistrationStatus.ATTENDED) {
            throw new DomainException("Cannot cancel a registration after checking in.");
        }
        if (event.getStatus() == EventStatus.COMPLETED) {
            throw new DomainException("Cannot cancel a registration for a completed event.");
        }

        boolean freedConfirmedSeat = (this.status == RegistrationStatus.REGISTERED);
        this.status = RegistrationStatus.CANCELLED;
        this.waitlistPosition = null;
        this.promotedByThisCancellation = null;

        if (freedConfirmedSeat) {
            this.promotedByThisCancellation = event.promoteNextWaitlisted();
        } else {
            event.resequenceWaitlist();
        }
        return this.status;
    }

    /**
     * UC4 & UC9 / SD-04 & SD-09 (GRASP Creator): Creates Attendance and sets status = ATTENDED.
     */
    public Attendance markAttended(CheckInMethod method) {
        if (method == null) {
            throw new DomainException("Check-in method is required.");
        }
        if (status == RegistrationStatus.CANCELLED) {
            throw new DomainException("Cannot check in a cancelled registration.");
        }
        if (status == RegistrationStatus.WAITLISTED) {
            throw new DomainException("Waitlisted students cannot check in until promoted to REGISTERED.");
        }
        if (status == RegistrationStatus.ATTENDED && attendance != null) {
            if (method == CheckInMethod.CODE) {
                throw new DomainException("You have already checked in to this event.");
            }
            return attendance;
        }
        this.attendance = new Attendance(method);
        this.status = RegistrationStatus.ATTENDED;
        this.waitlistPosition = null;
        return this.attendance;
    }

    public boolean isAttended() {
        return status == RegistrationStatus.ATTENDED || attendance != null;
    }

    /**
     * UC12 / SD-12: Applies admin's conflict resolution to this registration.
     */
    public void applyResolution(ResolutionType resolution) {
        if (resolution == null) {
            throw new DomainException("Resolution type is required.");
        }
        switch (resolution) {
            case KEEP_REGISTERED, INCREASE_CAPACITY -> {
                if (status != RegistrationStatus.ATTENDED) {
                    this.status = RegistrationStatus.REGISTERED;
                    this.waitlistPosition = null;
                }
                event.resequenceWaitlist();
            }
            case MOVE_TO_WAITLIST -> {
                if (status == RegistrationStatus.ATTENDED) {
                    throw new DomainException("Cannot move an already-attended student to the waitlist.");
                }
                int nextPos = event.getWaitlist().size() + 1;
                this.status = RegistrationStatus.WAITLISTED;
                this.waitlistPosition = nextPos;
                event.resequenceWaitlist();
            }
            case CANCEL_REGISTRATION -> {
                if (status == RegistrationStatus.ATTENDED) {
                    throw new DomainException("Cannot cancel an already-attended registration.");
                }
                this.status = RegistrationStatus.CANCELLED;
                this.waitlistPosition = null;
                event.resequenceWaitlist();
            }
            case OTHER -> event.resequenceWaitlist();
        }
    }

    public RegistrationSummary toSummary() {
        return SummaryFactory.createRegistrationSummary(this);
    }

    public AttendanceRow toAttendanceRow() {
        return new AttendanceRow(
                registrationId,
                student.getUserId(),
                student.getFullName(),
                student.getRollNumber(),
                status,
                isAttended(),
                attendance != null ? attendance.getCheckedInAt() : null,
                attendance != null ? attendance.getMethod() : null
        );
    }

    public Participant toParticipant() {
        return new Participant(
                student.getFullName(),
                student.getRollNumber(),
                status,
                attendance != null ? attendance.getCheckedInAt() : null,
                attendance != null ? attendance.getMethod() : null
        );
    }

    public Registration getPromotedByThisCancellation() {
        return promotedByThisCancellation;
    }

    public int getRegistrationId() { return registrationId; }
    public Student getStudent() { return student; }
    public Event getEvent() { return event; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public RegistrationStatus getStatus() { return status; }
    public Integer getWaitlistPosition() { return waitlistPosition; }
    public Attendance getAttendance() { return attendance; }
}
