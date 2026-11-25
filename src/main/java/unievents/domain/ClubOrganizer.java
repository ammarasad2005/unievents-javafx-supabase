package unievents.domain;

import unievents.shared.AudienceType;
import unievents.shared.DomainException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClubOrganizer extends User {
    private boolean isPresident;
    private Club club;
    private final List<Club> managedClubs = new ArrayList<>();

    public ClubOrganizer(int userId, String fullName, String email, String passwordHash,
                         boolean isActive, boolean isPresident) {
        super(userId, fullName, email, passwordHash, isActive);
        this.isPresident = isPresident;
    }

    public boolean isPresident() {
        return isPresident;
    }

    public Club getClub() {
        return club;
    }

    public List<Club> getManagedClubs() {
        return Collections.unmodifiableList(managedClubs);
    }

    public void assignClub(Club assignedClub) {
        if (assignedClub != null) {
            if (this.club == null) {
                this.club = assignedClub;
            }
            if (!managedClubs.contains(assignedClub)) {
                managedClubs.add(assignedClub);
            }
        }
    }

    public void assignAsPresident(Club assignedClub) {
        if (assignedClub == null) {
            throw new DomainException("Cannot assign president to a null club.");
        }
        this.isPresident = true;
        this.club = assignedClub;
        if (!managedClubs.contains(assignedClub)) {
            managedClubs.add(assignedClub);
        }
    }

    public List<Event> getMyEvents() {
        if (managedClubs.isEmpty()) {
            return Collections.emptyList();
        }
        List<Event> result = new ArrayList<>();
        for (Club c : managedClubs) {
            for (Event ev : c.getEvents()) {
                if (!result.contains(ev)) {
                    result.add(ev);
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    public Announcement createAnnouncement(Event event, String subject, String message, AudienceType audience) {
        if (event == null) {
            throw new DomainException("Target event is required to send an announcement.");
        }
        return new Announcement(this, event, subject, message, audience);
    }

    @Override
    public String getRole() {
        return "CLUB_ORGANIZER";
    }

    @Override
    public String getRoleDisplayName() {
        return isPresident ? "Club President" : "Club Organizer";
    }

    @Override
    public String getProfileSummary() {
        String clubLabel = club != null ? club.getName() : "No Club Assigned";
        return getFullName() + " · " + (isPresident ? "President, " : "Organizer, ") + clubLabel;
    }

    @Override
    public <T> T accept(UserRoleVisitor<T> visitor) {
        return visitor.visitOrganizer(this);
    }

    @Override
    public String toString() {
        return getFullName() + " (" + getEmail() + (isPresident ? " · President" : "") + ")";
    }
}
