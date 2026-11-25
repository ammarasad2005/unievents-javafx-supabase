package unievents.application;

import unievents.domain.Announcement;
import unievents.domain.ClubOrganizer;
import unievents.domain.Event;
import unievents.domain.Student;
import unievents.infrastructure.persistence.DataHandler;
import unievents.shared.AudienceCounts;
import unievents.shared.AudienceType;
import unievents.shared.DomainException;

import java.util.List;

/**
 * UC8 / SD-08: Send Event Announcement & Notify Selected Audience.
 */
public class AnnouncementController {
    private final DataHandler dataHandler;
    private final ClubOrganizer organizer;

    public AnnouncementController(DataHandler dataHandler, ClubOrganizer organizer) {
        this.dataHandler = dataHandler;
        this.organizer = organizer;
    }

    public AudienceCounts getAudienceCounts(int eventId) {
        ClubOrganizer freshOrg = requireFreshOrganizer();
        Event event = EventManagementController.requireOwnedEvent(dataHandler, freshOrg, eventId);
        return event.getAudienceCounts();
    }

    public Announcement sendAnnouncement(int eventId, String subject, String message, AudienceType audience) {
        ClubOrganizer freshOrg = requireFreshOrganizer();
        Event event = EventManagementController.requireOwnedEvent(dataHandler, freshOrg, eventId);

        List<Student> recipients = event.getAudience(audience);
        Announcement announcement = freshOrg.createAnnouncement(event, subject, message, audience);
        announcement.notifyRecipients(recipients);
        dataHandler.saveAnnouncement(announcement);
        return announcement;
    }

    private ClubOrganizer requireFreshOrganizer() {
        ClubOrganizer fresh = dataHandler.findOrganizerById(organizer.getUserId());
        if (fresh == null) {
            throw new DomainException("Organizer session could not be verified.");
        }
        return fresh;
    }
}
