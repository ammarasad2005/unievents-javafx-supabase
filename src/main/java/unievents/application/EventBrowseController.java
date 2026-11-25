package unievents.application;

import unievents.domain.Category;
import unievents.domain.Club;
import unievents.domain.Event;
import unievents.infrastructure.persistence.DataHandler;
import unievents.shared.DomainException;
import unievents.shared.EventDetails;
import unievents.shared.EventSummary;

import java.time.LocalDateTime;
import java.util.List;

/**
 * UC1 / SD-01: Browse & Filter Published Events, and inspect Event Details.
 */
public class EventBrowseController {
    private final DataHandler dataHandler;

    public EventBrowseController(DataHandler dataHandler) {
        this.dataHandler = dataHandler;
    }

    public List<EventSummary> getPublishedEvents() {
        return dataHandler.findPublishedEvents().stream()
                .map(Event::toSummary)
                .toList();
    }

    public List<EventSummary> filterEvents(Integer categoryId, Integer clubId, LocalDateTime date) {
        if ((categoryId == null || categoryId <= 0)
                && (clubId == null || clubId <= 0)
                && date == null) {
            return getPublishedEvents();
        }
        return dataHandler.findEventsByFilters(categoryId, clubId, date).stream()
                .map(Event::toSummary)
                .toList();
    }

    public EventDetails getEventDetails(int eventId) {
        Event event = dataHandler.findEventById(eventId);
        if (event == null) {
            throw new DomainException("Event #" + eventId + " was not found.");
        }
        return event.toDetails();
    }

    public List<Category> getCategories() {
        return dataHandler.findAllCategories();
    }

    public List<Club> getClubs() {
        return dataHandler.findAllClubs();
    }
}
