package unievents.domain;

import unievents.shared.AudienceType;
import unievents.shared.DomainException;
import unievents.shared.NotificationType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Announcement {
    private int announcementId;
    private final ClubOrganizer organizer;
    private final Event event;
    private final String subject;
    private final String message;
    private final AudienceType audience;
    private final LocalDateTime sentAt;
    private final List<Notification> generatedNotifications = new ArrayList<>();

    public Announcement(ClubOrganizer organizer, Event event, String subject, String message, AudienceType audience) {
        this(0, organizer, event, subject, message, audience, LocalDateTime.now());
    }

    public Announcement(int announcementId, ClubOrganizer organizer, Event event,
                        String subject, String message, AudienceType audience, LocalDateTime sentAt) {
        if (organizer == null) {
            throw new DomainException("Organizer is required to send an announcement.");
        }
        if (event == null) {
            throw new DomainException("Event is required to send an announcement.");
        }
        if (subject == null || subject.isBlank()) {
            throw new DomainException("Announcement subject cannot be empty.");
        }
        if (message == null || message.isBlank()) {
            throw new DomainException("Announcement message cannot be empty.");
        }
        if (audience == null) {
            throw new DomainException("Announcement audience is required.");
        }
        this.announcementId = announcementId;
        this.organizer = organizer;
        this.event = event;
        this.subject = subject.trim();
        this.message = message.trim();
        this.audience = audience;
        this.sentAt = sentAt != null ? sentAt : LocalDateTime.now();
    }

    public void assignAnnouncementId(int id) {
        if (this.announcementId == 0 && id > 0) {
            this.announcementId = id;
        }
    }

    /**
     * SD-08: Creates in-app Notification objects for every recipient student.
     */
    public int notifyRecipients(List<Student> students) {
        generatedNotifications.clear();
        if (students == null || students.isEmpty()) {
            return 0;
        }
        String formattedMessage = "[" + event.getTitle() + "] " + subject + " — " + message;
        for (Student student : students) {
            Notification notification = new Notification(student, formattedMessage, NotificationType.ANNOUNCEMENT);
            student.receiveNotification(notification);
            generatedNotifications.add(notification);
        }
        return generatedNotifications.size();
    }

    public int getNotifiedCount() {
        return generatedNotifications.size();
    }

    public List<Notification> getGeneratedNotifications() {
        return Collections.unmodifiableList(generatedNotifications);
    }

    public int getAnnouncementId() { return announcementId; }
    public ClubOrganizer getOrganizer() { return organizer; }
    public Event getEvent() { return event; }
    public String getSubject() { return subject; }
    public String getMessage() { return message; }
    public AudienceType getAudience() { return audience; }
    public LocalDateTime getSentAt() { return sentAt; }
}
