package unievents.application;

import unievents.domain.Category;
import unievents.domain.Club;
import unievents.domain.ClubOrganizer;
import unievents.domain.Event;
import unievents.domain.Notification;
import unievents.domain.Student;
import unievents.infrastructure.persistence.DataHandler;
import unievents.infrastructure.persistence.DatabaseController;
import unievents.shared.AudienceType;
import unievents.shared.DomainException;
import unievents.shared.EventDetails;
import unievents.shared.EventStatus;
import unievents.shared.EventSummary;

import java.time.LocalDateTime;
import java.util.List;

/**
 * UC6 & UC7 / SD-06 & SD-07: Organizer Create/Edit Event Details & Manage Event Status Transitions.
 */
public class EventManagementController {
    private final DataHandler dataHandler;
    private final ClubOrganizer organizer;

    public EventManagementController(DataHandler dataHandler, ClubOrganizer organizer) {
        this.dataHandler = dataHandler;
        this.organizer = organizer;
    }

    public List<EventSummary> getMyEvents() {
        ClubOrganizer freshOrg = requireFreshOrganizer();
        return freshOrg.getMyEvents().stream()
                .map(Event::toManagementSummary)
                .toList();
    }

    public EventDetails getEventDetails(int eventId) {
        ClubOrganizer freshOrg = requireFreshOrganizer();
        Event event = requireOwnedEvent(freshOrg, eventId);
        return event.toManagementDetails();
    }

    public Event saveEvent(Integer eventId, String title, String description,
                           LocalDateTime startDateTime, LocalDateTime endDateTime,
                           String venue, int capacity, int categoryId) {
        Category category = dataHandler.findCategoryById(categoryId);
        if (category == null) {
            throw new DomainException("Please select a valid event category.");
        }
        ClubOrganizer freshOrg = requireFreshOrganizer();

        if (eventId == null || eventId <= 0) {
            Club club = freshOrg.getClub();
            if (club == null) {
                throw new DomainException("You are not assigned to an active club yet. Ask an Admin to assign you.");
            }
            Event newEvent = club.createEvent(title, description, startDateTime, endDateTime, venue, capacity, category);
            dataHandler.saveEvent(newEvent);
            return newEvent;
        } else {
            Event existingEvent = requireOwnedEvent(freshOrg, eventId);
            existingEvent.updateDetails(title, description, startDateTime, endDateTime, venue, capacity, category);
            dataHandler.saveEvent(existingEvent);
            return existingEvent;
        }
    }

    public EventStatus changeEventStatus(int eventId, EventStatus newStatus, String reason) {
        ClubOrganizer freshOrg = requireFreshOrganizer();
        Event event = requireOwnedEvent(freshOrg, eventId);

        EventStatus updatedStatus = event.changeStatus(newStatus, reason);

        DatabaseController db = DatabaseController.getInstance();
        db.beginTransaction();
        try {
            dataHandler.saveEvent(event);
            if (updatedStatus == EventStatus.CANCELLED) {
                for (Student recipient : event.getAudience(AudienceType.BOTH)) {
                    List<Notification> notifs = recipient.getNotifications();
                    if (!notifs.isEmpty()) {
                        dataHandler.saveNotification(notifs.get(notifs.size() - 1));
                    }
                }
            }
            db.commit();
            return updatedStatus;
        } catch (RuntimeException e) {
            db.rollback();
            throw e;
        }
    }

    public List<Category> getCategories() {
        return dataHandler.findAllCategories();
    }

    private ClubOrganizer requireFreshOrganizer() {
        ClubOrganizer fresh = dataHandler.findOrganizerById(organizer.getUserId());
        if (fresh == null) {
            throw new DomainException("Organizer session could not be verified.");
        }
        return fresh;
    }

    static Event requireOwnedEvent(DataHandler dataHandler, ClubOrganizer freshOrg, int eventId) {
        Event event = dataHandler.findEventById(eventId);
        if (event == null) {
            throw new DomainException("Event #" + eventId + " was not found.");
        }
        boolean ownsClub = freshOrg.getManagedClubs().stream()
                .anyMatch(c -> c.getClubId() == event.getClub().getClubId());
        if (!ownsClub) {
            throw new DomainException("Access denied: event \"" + event.getTitle() + "\" belongs to another club.");
        }
        return event;
    }

    private Event requireOwnedEvent(ClubOrganizer freshOrg, int eventId) {
        return requireOwnedEvent(dataHandler, freshOrg, eventId);
    }
}
