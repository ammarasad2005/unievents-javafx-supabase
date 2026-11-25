package unievents.domain;

import unievents.shared.DomainException;
import unievents.shared.EventStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Club {
    private int clubId;
    private final String name;
    private final String description;
    private boolean isActive;
    private final Admin createdBy;
    private final List<ClubOrganizer> organizers = new ArrayList<>();
    private final List<Event> events = new ArrayList<>();

    public Club(String name, String description) {
        this(0, name, description, true, null);
    }

    public Club(int clubId, String name, String description, boolean isActive, Admin createdBy) {
        if (name == null || name.isBlank()) {
            throw new DomainException("Club name is required.");
        }
        this.clubId = clubId;
        this.name = name.trim();
        this.description = description != null ? description.trim() : "";
        this.isActive = isActive;
        this.createdBy = createdBy;
    }

    public void assignClubId(int id) {
        if (this.clubId == 0 && id > 0) {
            this.clubId = id;
        }
    }

    public void addOrganizer(ClubOrganizer organizer) {
        if (organizer == null) {
            throw new DomainException("Organizer cannot be null.");
        }
        if (!organizers.contains(organizer)) {
            organizers.add(organizer);
        }
        organizer.assignClub(this);
    }

    public void attachHydratedEvent(Event event) {
        if (event != null && !events.contains(event)) {
            events.add(event);
        }
    }

    /**
     * GRASP Creator (SD-06): Club creates Event in DRAFT status with a generated check-in code.
     */
    public Event createEvent(String title, String description,
                             LocalDateTime startDateTime, LocalDateTime endDateTime,
                             String venue, int capacity, Category category) {
        String codePrefix = name.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (codePrefix.length() > 3) {
            codePrefix = codePrefix.substring(0, 3);
        } else if (codePrefix.isEmpty()) {
            codePrefix = "UNI";
        }
        String checkInCode = codePrefix + "-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();

        Event event = new Event(
                0,
                this,
                category,
                title,
                description,
                startDateTime,
                endDateTime,
                venue,
                capacity,
                EventStatus.DRAFT,
                checkInCode
        );
        events.add(event);
        return event;
    }

    public int getClubId() { return clubId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isActive() { return isActive; }
    public Admin getCreatedBy() { return createdBy; }
    public List<ClubOrganizer> getOrganizers() { return Collections.unmodifiableList(organizers); }
    public List<Event> getEvents() { return Collections.unmodifiableList(events); }

    @Override
    public String toString() {
        return name;
    }
}
