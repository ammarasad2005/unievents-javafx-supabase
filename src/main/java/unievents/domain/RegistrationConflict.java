package unievents.domain;

import unievents.shared.ConflictDetails;
import unievents.shared.ConflictStatus;
import unievents.shared.ConflictSummary;
import unievents.shared.ConflictType;
import unievents.shared.DomainException;
import unievents.shared.ResolutionType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class RegistrationConflict {
    private int conflictId;
    private final Event event;
    private final ConflictType type;
    private ConflictStatus status;
    private final LocalDateTime detectedAt;
    private ResolutionType resolution;
    private String reason;
    private LocalDateTime resolvedAt;
    private Admin resolvedBy;
    private final List<Registration> registrations = new ArrayList<>();

    public RegistrationConflict(int conflictId, Event event, ConflictType type,
                                ConflictStatus status, LocalDateTime detectedAt,
                                ResolutionType resolution, String reason,
                                LocalDateTime resolvedAt, Admin resolvedBy,
                                List<Registration> concernedRegistrations) {
        if (event == null) {
            throw new DomainException("Conflict must reference an event.");
        }
        if (type == null) {
            throw new DomainException("Conflict type is required.");
        }
        if (status == null) {
            throw new DomainException("Conflict status is required.");
        }
        this.conflictId = conflictId;
        this.event = event;
        this.type = type;
        this.status = status;
        this.detectedAt = detectedAt != null ? detectedAt : LocalDateTime.now();
        this.resolution = resolution;
        this.reason = reason;
        this.resolvedAt = resolvedAt;
        this.resolvedBy = resolvedBy;
        if (concernedRegistrations != null) {
            this.registrations.addAll(concernedRegistrations);
        }
    }

    public void assignConflictId(int id) {
        if (this.conflictId == 0 && id > 0) {
            this.conflictId = id;
        }
    }

    /**
     * UC12 / SD-12: Resolves an open registration conflict and applies the chosen policy to concerned registrations.
     */
    public ConflictStatus resolve(Admin admin, ResolutionType resolution, String reason) {
        if (admin == null) {
            throw new DomainException("Admin authority is required to resolve a registration conflict.");
        }
        if (this.status != ConflictStatus.OPEN) {
            throw new DomainException("Conflict #" + conflictId + " is already " + this.status + ".");
        }
        if (resolution == null) {
            throw new DomainException("A resolution action must be selected.");
        }
        if (reason == null || reason.isBlank()) {
            throw new DomainException("A resolution reason is required for auditability.");
        }

        if (resolution == ResolutionType.INCREASE_CAPACITY) {
            int neededCapacity = Math.max(event.getCapacity() + 1, event.getRegisteredCount());
            event.adjustCapacityForConflictResolution(neededCapacity);
        }

        for (Registration reg : registrations) {
            reg.applyResolution(resolution);
        }

        this.resolvedBy = admin;
        this.resolution = resolution;
        this.reason = reason.trim();
        this.status = ConflictStatus.RESOLVED;
        this.resolvedAt = LocalDateTime.now();
        return this.status;
    }

    public String getEventTitle() {
        return event.getTitle();
    }

    public String getStudentName() {
        if (registrations.isEmpty()) {
            return "Multiple / Unspecified";
        }
        return registrations.stream()
                .map(r -> r.getStudent().getFullName())
                .collect(Collectors.joining(", "));
    }

    public ConflictSummary toSummary() {
        return SummaryFactory.createConflictSummary(this);
    }

    public ConflictDetails toDetails() {
        return SummaryFactory.createConflictDetails(this);
    }

    public int getConflictId() { return conflictId; }
    public Event getEvent() { return event; }
    public ConflictType getType() { return type; }
    public ConflictStatus getStatus() { return status; }
    public LocalDateTime getDetectedAt() { return detectedAt; }
    public ResolutionType getResolution() { return resolution; }
    public String getReason() { return reason; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public Admin getResolvedBy() { return resolvedBy; }
    public List<Registration> getRegistrations() { return Collections.unmodifiableList(registrations); }
}
