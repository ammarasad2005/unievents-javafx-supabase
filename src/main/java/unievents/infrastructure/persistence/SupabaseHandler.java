package unievents.infrastructure.persistence;

import unievents.domain.Admin;
import unievents.domain.Announcement;
import unievents.domain.Attendance;
import unievents.domain.AttendanceReport;
import unievents.domain.Category;
import unievents.domain.Club;
import unievents.domain.ClubOrganizer;
import unievents.domain.Event;
import unievents.domain.Notification;
import unievents.domain.Registration;
import unievents.domain.RegistrationConflict;
import unievents.domain.Review;
import unievents.domain.Student;
import unievents.domain.User;
import unievents.shared.AudienceType;
import unievents.shared.CheckInMethod;
import unievents.shared.ConflictStatus;
import unievents.shared.ConflictType;
import unievents.shared.DataAccessException;
import unievents.shared.DomainException;
import unievents.shared.EventStatus;
import unievents.shared.NotificationType;
import unievents.shared.RegistrationStatus;
import unievents.shared.ResolutionType;
import unievents.shared.UserRole;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PostgreSQL / Supabase JDBC implementation of DataHandler.
 * Replaces MySQLHandler from Deliverable 5 while preserving the exact DataHandler contract.
 */
public class SupabaseHandler implements DataHandler {

    private final DatabaseController db;

    public SupabaseHandler() {
        this(DatabaseController.getInstance());
    }

    public SupabaseHandler(DatabaseController db) {
        this.db = db;
    }

    // =========================================================================
    // Events
    // =========================================================================

    private static final String BASE_EVENT_SELECT = """
            SELECT e.event_id, e.title, e.description, e.start_date_time, e.end_date_time,
                   e.venue, e.capacity, e.status, e.check_in_code,
                   c.club_id, c.name AS club_name, c.description AS club_desc, c.is_active AS club_active,
                   cat.category_id, cat.name AS category_name
            FROM events e
            JOIN clubs c ON c.club_id = e.club_id
            JOIN categories cat ON cat.category_id = e.category_id
            """;

    @Override
    public Event findEventById(int eventId) {
        String sql = BASE_EVENT_SELECT + " WHERE e.event_id = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Event event = mapEventRow(rs, new LinkedHashMap<>(), new LinkedHashMap<>());
                hydrateEventsDetails(Map.of(event.getEventId(), event));
                return event;
            }
        } catch (SQLException e) {
            throw translateSqlException("Failed to load event #" + eventId, e);
        }
    }

    @Override
    public List<Event> findPublishedEvents() {
        String sql = BASE_EVENT_SELECT + " WHERE e.status = 'PUBLISHED'::event_status ORDER BY e.start_date_time ASC";
        return queryEvents(sql, ps -> {});
    }

    @Override
    public List<Event> findEventsByFilters(Integer categoryId, Integer clubId, LocalDateTime date) {
        StringBuilder sql = new StringBuilder(BASE_EVENT_SELECT)
                .append(" WHERE e.status = 'PUBLISHED'::event_status");
        List<StatementBinder> paramBinders = new ArrayList<>();

        if (categoryId != null && categoryId > 0) {
            sql.append(" AND e.category_id = ?");
            final int paramIdx = paramBinders.size() + 1;
            paramBinders.add(ps -> ps.setInt(paramIdx, categoryId));
        }
        if (clubId != null && clubId > 0) {
            sql.append(" AND e.club_id = ?");
            final int paramIdx = paramBinders.size() + 1;
            paramBinders.add(ps -> ps.setInt(paramIdx, clubId));
        }
        if (date != null) {
            LocalDate day = date.toLocalDate();
            Timestamp startOfDay = Timestamp.valueOf(day.atStartOfDay());
            Timestamp endOfDay = Timestamp.valueOf(day.plusDays(1).atStartOfDay());
            sql.append(" AND e.start_date_time >= ? AND e.start_date_time < ?");
            final int startIdx = paramBinders.size() + 1;
            final int endIdx = paramBinders.size() + 2;
            paramBinders.add(ps -> ps.setTimestamp(startIdx, startOfDay));
            paramBinders.add(ps -> ps.setTimestamp(endIdx, endOfDay));
        }
        sql.append(" ORDER BY e.start_date_time ASC");

        return queryEvents(sql.toString(), ps -> {
            for (StatementBinder binder : paramBinders) {
                binder.bind(ps);
            }
        });
    }

    @Override
    public void saveEvent(Event event) {
        if (event == null) {
            throw new DomainException("Cannot save a null event.");
        }
        if (event.getEventId() <= 0) {
            String insertSql = """
                    INSERT INTO events (club_id, category_id, title, description,
                                        start_date_time, end_date_time, venue, capacity, status, check_in_code)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::event_status, ?)
                    RETURNING event_id
                    """;
            try (PreparedStatement ps = db.getConnection().prepareStatement(insertSql)) {
                ps.setInt(1, event.getClub().getClubId());
                ps.setInt(2, event.getCategory().getCategoryId());
                ps.setString(3, event.getTitle());
                ps.setString(4, event.getDescription());
                ps.setTimestamp(5, Timestamp.valueOf(event.getStartDateTime()));
                ps.setTimestamp(6, Timestamp.valueOf(event.getEndDateTime()));
                ps.setString(7, event.getVenue());
                ps.setInt(8, event.getCapacity());
                ps.setString(9, event.getStatus().name());
                ps.setString(10, event.getCheckInCode());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        event.assignEventId(rs.getInt(1));
                    }
                }
            } catch (SQLException e) {
                throw translateSqlException("Failed to create event \"" + event.getTitle() + "\"", e);
            }
        } else {
            String updateSql = """
                    UPDATE events
                    SET category_id = ?, title = ?, description = ?,
                        start_date_time = ?, end_date_time = ?, venue = ?,
                        capacity = ?, status = ?::event_status
                    WHERE event_id = ?
                    """;
            try (PreparedStatement ps = db.getConnection().prepareStatement(updateSql)) {
                ps.setInt(1, event.getCategory().getCategoryId());
                ps.setString(2, event.getTitle());
                ps.setString(3, event.getDescription());
                ps.setTimestamp(4, Timestamp.valueOf(event.getStartDateTime()));
                ps.setTimestamp(5, Timestamp.valueOf(event.getEndDateTime()));
                ps.setString(6, event.getVenue());
                ps.setInt(7, event.getCapacity());
                ps.setString(8, event.getStatus().name());
                ps.setInt(9, event.getEventId());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw translateSqlException("Failed to update event \"" + event.getTitle() + "\"", e);
            }
        }
    }

    // =========================================================================
    // Categories & Clubs
    // =========================================================================

    @Override
    public Category findCategoryById(int categoryId) {
        String sql = "SELECT category_id, name FROM categories WHERE category_id = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Category(rs.getInt("category_id"), rs.getString("name"));
                }
                return null;
            }
        } catch (SQLException e) {
            throw translateSqlException("Failed to load category #" + categoryId, e);
        }
    }

    @Override
    public List<Category> findAllCategories() {
        String sql = "SELECT category_id, name FROM categories ORDER BY category_id ASC";
        List<Category> list = new ArrayList<>();
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Category(rs.getInt("category_id"), rs.getString("name")));
            }
            return list;
        } catch (SQLException e) {
            throw translateSqlException("Failed to load categories", e);
        }
    }

    @Override
    public List<Club> findAllClubs() {
        String sql = "SELECT club_id, name, description, is_active FROM clubs WHERE is_active = TRUE ORDER BY name ASC";
        List<Club> clubs = new ArrayList<>();
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                clubs.add(new Club(
                        rs.getInt("club_id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getBoolean("is_active"),
                        null
                ));
            }
            return clubs;
        } catch (SQLException e) {
            throw translateSqlException("Failed to load clubs", e);
        }
    }

    @Override
    public void saveClub(Club club) {
        if (club == null) {
            throw new DomainException("Cannot save a null club.");
        }
        boolean ownTx = !db.isInTransaction();
        if (ownTx) {
            db.beginTransaction();
        }
        try {
            Connection conn = db.getConnection();
            if (club.getClubId() <= 0) {
                String insertClub = """
                        INSERT INTO clubs (name, description, is_active, created_by)
                        VALUES (?, ?, ?, ?)
                        RETURNING club_id
                        """;
                try (PreparedStatement ps = conn.prepareStatement(insertClub)) {
                    ps.setString(1, club.getName());
                    ps.setString(2, club.getDescription());
                    ps.setBoolean(3, club.isActive());
                    if (club.getCreatedBy() != null && club.getCreatedBy().getUserId() > 0) {
                        ps.setInt(4, club.getCreatedBy().getUserId());
                    } else {
                        ps.setNull(4, Types.INTEGER);
                    }
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            club.assignClubId(rs.getInt(1));
                        }
                    }
                }
            } else {
                String updateClub = "UPDATE clubs SET name = ?, description = ?, is_active = ? WHERE club_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateClub)) {
                    ps.setString(1, club.getName());
                    ps.setString(2, club.getDescription());
                    ps.setBoolean(3, club.isActive());
                    ps.setInt(4, club.getClubId());
                    ps.executeUpdate();
                }
            }

            // Persist organizer assignments & president flag
            for (ClubOrganizer org : club.getOrganizers()) {
                String assignSql = """
                        INSERT INTO club_organizer_assignments (club_id, organizer_id)
                        VALUES (?, ?)
                        ON CONFLICT (club_id, organizer_id) DO NOTHING
                        """;
                try (PreparedStatement ps = conn.prepareStatement(assignSql)) {
                    ps.setInt(1, club.getClubId());
                    ps.setInt(2, org.getUserId());
                    ps.executeUpdate();
                }
                String updateOrgSql = "UPDATE club_organizers SET is_president = ? WHERE user_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateOrgSql)) {
                    ps.setBoolean(1, org.isPresident());
                    ps.setInt(2, org.getUserId());
                    ps.executeUpdate();
                }
            }

            if (ownTx) {
                db.commit();
            }
        } catch (SQLException e) {
            if (ownTx) {
                db.rollback();
            }
            if ("23505".equals(e.getSQLState())) {
                throw new DomainException("A club named \"" + club.getName() + "\" already exists.");
            }
            throw translateSqlException("Failed to save club \"" + club.getName() + "\"", e);
        } catch (RuntimeException e) {
            if (ownTx) {
                db.rollback();
            }
            throw e;
        }
    }

    // =========================================================================
    // Registrations & Attendance
    // =========================================================================

    @Override
    public void saveRegistration(Registration registration) {
        if (registration == null) {
            throw new DomainException("Cannot save a null registration.");
        }
        boolean ownTx = !db.isInTransaction();
        if (ownTx) {
            db.beginTransaction();
        }
        try {
            persistSingleRegistration(db.getConnection(), registration);
            if (ownTx) {
                db.commit();
            }
        } catch (SQLException e) {
            if (ownTx) {
                db.rollback();
            }
            throw translateSqlException("Failed to save registration", e);
        } catch (RuntimeException e) {
            if (ownTx) {
                db.rollback();
            }
            throw e;
        }
    }

    @Override
    public void saveRegistrations(List<Registration> registrations) {
        if (registrations == null || registrations.isEmpty()) {
            return;
        }
        boolean ownTx = !db.isInTransaction();
        if (ownTx) {
            db.beginTransaction();
        }
        try {
            Connection conn = db.getConnection();
            for (Registration reg : registrations) {
                persistSingleRegistration(conn, reg);
            }
            if (ownTx) {
                db.commit();
            }
        } catch (SQLException e) {
            if (ownTx) {
                db.rollback();
            }
            throw translateSqlException("Failed to save registrations batch", e);
        } catch (RuntimeException e) {
            if (ownTx) {
                db.rollback();
            }
            throw e;
        }
    }

    private void persistSingleRegistration(Connection conn, Registration registration) throws SQLException {
        if (registration.getRegistrationId() <= 0) {
            String upsertSql = """
                    INSERT INTO registrations (event_id, student_id, registered_at, status, waitlist_position)
                    VALUES (?, ?, ?, ?::registration_status, ?)
                    ON CONFLICT (event_id, student_id) DO UPDATE
                      SET registered_at = EXCLUDED.registered_at,
                          status = EXCLUDED.status,
                          waitlist_position = EXCLUDED.waitlist_position
                    RETURNING registration_id
                    """;
            try (PreparedStatement ps = conn.prepareStatement(upsertSql)) {
                ps.setInt(1, registration.getEvent().getEventId());
                ps.setInt(2, registration.getStudent().getUserId());
                ps.setTimestamp(3, Timestamp.valueOf(registration.getRegisteredAt()));
                ps.setString(4, registration.getStatus().name());
                if (registration.getWaitlistPosition() != null) {
                    ps.setInt(5, registration.getWaitlistPosition());
                } else {
                    ps.setNull(5, Types.INTEGER);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        registration.assignRegistrationId(rs.getInt(1));
                    }
                }
            }
        } else {
            String updateSql = """
                    UPDATE registrations
                    SET status = ?::registration_status,
                        waitlist_position = ?,
                        registered_at = ?
                    WHERE registration_id = ?
                    """;
            try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setString(1, registration.getStatus().name());
                if (registration.getWaitlistPosition() != null) {
                    ps.setInt(2, registration.getWaitlistPosition());
                } else {
                    ps.setNull(2, Types.INTEGER);
                }
                ps.setTimestamp(3, Timestamp.valueOf(registration.getRegisteredAt()));
                ps.setInt(4, registration.getRegistrationId());
                ps.executeUpdate();
            }
        }

        // Persist Attendance row if checked in
        Attendance att = registration.getAttendance();
        if (att != null && registration.isAttended()) {
            String attSql = """
                    INSERT INTO attendances (registration_id, checked_in_at, method)
                    VALUES (?, ?, ?::check_in_method)
                    ON CONFLICT (registration_id) DO UPDATE
                      SET checked_in_at = EXCLUDED.checked_in_at,
                          method = EXCLUDED.method
                    RETURNING attendance_id
                    """;
            try (PreparedStatement ps = conn.prepareStatement(attSql)) {
                ps.setInt(1, registration.getRegistrationId());
                ps.setTimestamp(2, Timestamp.valueOf(att.getCheckedInAt()));
                ps.setString(3, att.getMethod().name());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        att.assignAttendanceId(rs.getInt(1));
                    }
                }
            }
        }
    }

    // =========================================================================
    // Reviews, Announcements, Notifications & Reports
    // =========================================================================

    @Override
    public void saveReview(Review review) {
        if (review == null) {
            throw new DomainException("Cannot save a null review.");
        }
        String sql = """
                INSERT INTO reviews (event_id, student_id, rating, comment, posted_at)
                VALUES (?, ?, ?, ?, ?)
                RETURNING review_id
                """;
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, review.getEvent().getEventId());
            ps.setInt(2, review.getStudent().getUserId());
            ps.setInt(3, review.getRating());
            ps.setString(4, review.getComment());
            ps.setTimestamp(5, Timestamp.valueOf(review.getPostedAt()));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    review.assignReviewId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                throw new DomainException("You have already submitted a review for this event.");
            }
            throw translateSqlException("Failed to save event review", e);
        }
    }

    @Override
    public void saveAnnouncement(Announcement announcement) {
        if (announcement == null) {
            throw new DomainException("Cannot save a null announcement.");
        }
        boolean ownTx = !db.isInTransaction();
        if (ownTx) {
            db.beginTransaction();
        }
        try {
            Connection conn = db.getConnection();
            String insertAnn = """
                    INSERT INTO announcements (event_id, organizer_id, subject, message, audience, sent_at)
                    VALUES (?, ?, ?, ?, ?::audience_type, ?)
                    RETURNING announcement_id
                    """;
            try (PreparedStatement ps = conn.prepareStatement(insertAnn)) {
                ps.setInt(1, announcement.getEvent().getEventId());
                ps.setInt(2, announcement.getOrganizer().getUserId());
                ps.setString(3, announcement.getSubject());
                ps.setString(4, announcement.getMessage());
                ps.setString(5, announcement.getAudience().name());
                ps.setTimestamp(6, Timestamp.valueOf(announcement.getSentAt()));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        announcement.assignAnnouncementId(rs.getInt(1));
                    }
                }
            }

            String insertNotif = """
                    INSERT INTO notifications (user_id, message, type, created_at, is_read, announcement_id)
                    VALUES (?, ?, ?::notification_type, ?, ?, ?)
                    RETURNING notification_id
                    """;
            try (PreparedStatement ps = conn.prepareStatement(insertNotif)) {
                for (Notification n : announcement.getGeneratedNotifications()) {
                    ps.setInt(1, n.getRecipient().getUserId());
                    ps.setString(2, n.getMessage());
                    ps.setString(3, n.getType().name());
                    ps.setTimestamp(4, Timestamp.valueOf(n.getCreatedAt()));
                    ps.setBoolean(5, n.isRead());
                    ps.setInt(6, announcement.getAnnouncementId());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            n.assignNotificationId(rs.getInt(1));
                        }
                    }
                }
            }

            if (ownTx) {
                db.commit();
            }
        } catch (SQLException e) {
            if (ownTx) {
                db.rollback();
            }
            throw translateSqlException("Failed to save announcement", e);
        } catch (RuntimeException e) {
            if (ownTx) {
                db.rollback();
            }
            throw e;
        }
    }

    @Override
    public void saveNotification(Notification notification) {
        if (notification == null) {
            return;
        }
        String sql = """
                INSERT INTO notifications (user_id, message, type, created_at, is_read)
                VALUES (?, ?, ?::notification_type, ?, ?)
                RETURNING notification_id
                """;
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, notification.getRecipient().getUserId());
            ps.setString(2, notification.getMessage());
            ps.setString(3, notification.getType().name());
            ps.setTimestamp(4, Timestamp.valueOf(notification.getCreatedAt()));
            ps.setBoolean(5, notification.isRead());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    notification.assignNotificationId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw translateSqlException("Failed to save notification", e);
        }
    }

    @Override
    public List<Notification> findNotificationsByUserId(int userId) {
        User recipient = findUserById(userId);
        if (recipient == null) {
            return List.of();
        }
        String sql = """
                SELECT notification_id, message, type, created_at, is_read
                FROM notifications
                WHERE user_id = ?
                ORDER BY created_at DESC
                """;
        List<Notification> list = new ArrayList<>();
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Notification(
                            rs.getInt("notification_id"),
                            recipient,
                            rs.getString("message"),
                            NotificationType.valueOf(rs.getString("type")),
                            toLocalDateTime(rs.getTimestamp("created_at")),
                            rs.getBoolean("is_read")
                    ));
                }
            }
            return list;
        } catch (SQLException e) {
            throw translateSqlException("Failed to load notifications", e);
        }
    }

    @Override
    public void markNotificationsRead(int userId) {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE user_id = ? AND is_read = FALSE";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw translateSqlException("Failed to mark notifications as read", e);
        }
    }

    @Override
    public void saveAttendanceReport(AttendanceReport report) {
        if (report == null) {
            throw new DomainException("Cannot save a null attendance report.");
        }
        String sql = """
                INSERT INTO attendance_reports (event_id, organizer_id, generated_at, registered_count, attended_count)
                VALUES (?, ?, ?, ?, ?)
                RETURNING report_id
                """;
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, report.getEvent().getEventId());
            ps.setInt(2, report.getOrganizer().getUserId());
            ps.setTimestamp(3, Timestamp.valueOf(report.getGeneratedAt()));
            ps.setInt(4, report.getRegisteredCount());
            ps.setInt(5, report.getAttendedCount());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    report.assignReportId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw translateSqlException("Failed to save attendance report", e);
        }
    }

    // =========================================================================
    // Users, Students, Organizers, Admins
    // =========================================================================

    private static final String BASE_USER_SELECT = """
            SELECT u.user_id, u.full_name, u.email, u.password_hash, u.is_active, u.role,
                   s.roll_number, co.is_president, a.department
            FROM users u
            LEFT JOIN students s ON s.user_id = u.user_id
            LEFT JOIN club_organizers co ON co.user_id = u.user_id
            LEFT JOIN admins a ON a.user_id = u.user_id
            """;

    private User findUserById(int userId) {
        String sql = BASE_USER_SELECT + " WHERE u.user_id = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return mapUserRow(rs);
            }
        } catch (SQLException e) {
            throw translateSqlException("Failed to load user #" + userId, e);
        }
    }

    @Override
    public User findUserByEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        String sql = BASE_USER_SELECT + " WHERE LOWER(u.email) = LOWER(?)";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                User user = mapUserRow(rs);
                UserRole role = UserRole.valueOf(rs.getString("role"));
                if (role == UserRole.CLUB_ORGANIZER) {
                    return findOrganizerById(user.getUserId());
                } else if (role == UserRole.STUDENT) {
                    return findStudentById(user.getUserId());
                }
                return user;
            }
        } catch (SQLException e) {
            throw translateSqlException("Failed to look up user by email", e);
        }
    }

    @Override
    public List<User> findAllActiveUsers() {
        String sql = BASE_USER_SELECT + " WHERE u.is_active = TRUE ORDER BY u.user_id ASC";
        List<User> users = new ArrayList<>();
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(mapUserRow(rs));
            }
            return users;
        } catch (SQLException e) {
            throw translateSqlException("Failed to list active users", e);
        }
    }

    @Override
    public Student findStudentById(int userId) {
        String sql = BASE_USER_SELECT + " WHERE u.user_id = ? AND u.role = 'STUDENT'::user_role";
        Student student;
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                student = new Student(
                        rs.getInt("user_id"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getBoolean("is_active"),
                        rs.getString("roll_number")
                );
            }
        } catch (SQLException e) {
            throw translateSqlException("Failed to load student #" + userId, e);
        }

        // Hydrate all events for which this student has a registration so cancel/promote works on full Event graph
        String regEventIdsSql = """
                SELECT r.event_id
                FROM registrations r
                JOIN events e ON e.event_id = r.event_id
                WHERE r.student_id = ?
                ORDER BY e.start_date_time ASC, r.registration_id ASC
                """;
        List<Integer> eventIds = new ArrayList<>();
        try (PreparedStatement ps = db.getConnection().prepareStatement(regEventIdsSql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    eventIds.add(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw translateSqlException("Failed to load student registrations", e);
        }

        for (Integer evId : eventIds) {
            Event hydratedEvent = findEventById(evId);
            if (hydratedEvent != null) {
                Registration reg = hydratedEvent.findRegistrationFor(student);
                if (reg != null) {
                    student.addRegistration(reg);
                }
            }
        }
        return student;
    }

    @Override
    public ClubOrganizer findOrganizerById(int userId) {
        String sql = BASE_USER_SELECT + " WHERE u.user_id = ? AND u.role = 'CLUB_ORGANIZER'::user_role";
        ClubOrganizer organizer;
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                organizer = new ClubOrganizer(
                        rs.getInt("user_id"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getBoolean("is_active"),
                        rs.getBoolean("is_president")
                );
            }
        } catch (SQLException e) {
            throw translateSqlException("Failed to load organizer #" + userId, e);
        }

        // Load assigned clubs and hydrate each club's events
        String clubsSql = """
                SELECT c.club_id, c.name, c.description, c.is_active
                FROM clubs c
                JOIN club_organizer_assignments coa ON coa.club_id = c.club_id
                WHERE coa.organizer_id = ?
                ORDER BY c.club_id ASC
                """;
        List<Club> clubs = new ArrayList<>();
        try (PreparedStatement ps = db.getConnection().prepareStatement(clubsSql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Club club = new Club(
                            rs.getInt("club_id"),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getBoolean("is_active"),
                            null
                    );
                    club.addOrganizer(organizer);
                    clubs.add(club);
                }
            }
        } catch (SQLException e) {
            throw translateSqlException("Failed to load organizer's clubs", e);
        }

        for (Club club : clubs) {
            String evSql = BASE_EVENT_SELECT + " WHERE e.club_id = ? ORDER BY e.start_date_time DESC";
            List<Event> clubEvents = queryEventsWithPreloadedClub(evSql, club);
            for (Event ev : clubEvents) {
                club.attachHydratedEvent(ev);
            }
        }

        return organizer;
    }

    @Override
    public Admin findAdminById(int userId) {
        String sql = BASE_USER_SELECT + " WHERE u.user_id = ? AND u.role = 'ADMIN'::user_role";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new Admin(
                        rs.getInt("user_id"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getBoolean("is_active"),
                        rs.getString("department")
                );
            }
        } catch (SQLException e) {
            throw translateSqlException("Failed to load admin #" + userId, e);
        }
    }

    @Override
    public List<ClubOrganizer> findAllOrganizers() {
        String sql = BASE_USER_SELECT + " WHERE u.role = 'CLUB_ORGANIZER'::user_role AND u.is_active = TRUE ORDER BY u.full_name ASC";
        List<ClubOrganizer> organizers = new ArrayList<>();
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                organizers.add(new ClubOrganizer(
                        rs.getInt("user_id"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getBoolean("is_active"),
                        rs.getBoolean("is_president")
                ));
            }
            return organizers;
        } catch (SQLException e) {
            throw translateSqlException("Failed to load club organizers", e);
        }
    }

    // =========================================================================
    // Registration Conflicts
    // =========================================================================

    @Override
    public List<RegistrationConflict> findOpenConflicts() {
        String sql = """
                SELECT conflict_id, event_id, type, status, detected_at, resolution, reason, resolved_at, resolved_by
                FROM registration_conflicts
                WHERE status = 'OPEN'::conflict_status
                ORDER BY detected_at DESC, conflict_id ASC
                """;
        List<RegistrationConflict> conflicts = new ArrayList<>();
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                conflicts.add(hydrateConflictRow(rs));
            }
            return conflicts;
        } catch (SQLException e) {
            throw translateSqlException("Failed to load open registration conflicts", e);
        }
    }

    @Override
    public RegistrationConflict findConflictById(int conflictId) {
        String sql = """
                SELECT conflict_id, event_id, type, status, detected_at, resolution, reason, resolved_at, resolved_by
                FROM registration_conflicts
                WHERE conflict_id = ?
                """;
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, conflictId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return hydrateConflictRow(rs);
            }
        } catch (SQLException e) {
            throw translateSqlException("Failed to load conflict #" + conflictId, e);
        }
    }

    @Override
    public void saveConflict(RegistrationConflict conflict) {
        if (conflict == null) {
            throw new DomainException("Cannot save a null registration conflict.");
        }
        boolean ownTx = !db.isInTransaction();
        if (ownTx) {
            db.beginTransaction();
        }
        try {
            // Persist any capacity increase on Event + updated Registration statuses/waitlist positions
            saveEvent(conflict.getEvent());
            saveRegistrations(conflict.getEvent().getRegistrations());

            String sql = """
                    UPDATE registration_conflicts
                    SET status = ?::conflict_status,
                        resolution = ?::resolution_type,
                        reason = ?,
                        resolved_at = ?,
                        resolved_by = ?
                    WHERE conflict_id = ?
                    """;
            try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
                ps.setString(1, conflict.getStatus().name());
                if (conflict.getResolution() != null) {
                    ps.setString(2, conflict.getResolution().name());
                } else {
                    ps.setNull(2, Types.OTHER);
                }
                ps.setString(3, conflict.getReason());
                if (conflict.getResolvedAt() != null) {
                    ps.setTimestamp(4, Timestamp.valueOf(conflict.getResolvedAt()));
                } else {
                    ps.setNull(4, Types.TIMESTAMP);
                }
                if (conflict.getResolvedBy() != null && conflict.getResolvedBy().getUserId() > 0) {
                    ps.setInt(5, conflict.getResolvedBy().getUserId());
                } else {
                    ps.setNull(5, Types.INTEGER);
                }
                ps.setInt(6, conflict.getConflictId());
                ps.executeUpdate();
            }

            if (ownTx) {
                db.commit();
            }
        } catch (SQLException e) {
            if (ownTx) {
                db.rollback();
            }
            throw translateSqlException("Failed to save resolved conflict", e);
        } catch (RuntimeException e) {
            if (ownTx) {
                db.rollback();
            }
            throw e;
        }
    }

    // =========================================================================
    // Internal row mappers & batch hydration helpers (-mapRow(rs: ResultSet))
    // =========================================================================

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    private List<Event> queryEvents(String sql, StatementBinder binder) {
        Map<Integer, Event> eventMap = new LinkedHashMap<>();
        Map<Integer, Club> clubCache = new LinkedHashMap<>();
        Map<Integer, Category> catCache = new LinkedHashMap<>();

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Event event = mapEventRow(rs, clubCache, catCache);
                    eventMap.put(event.getEventId(), event);
                }
            }
            hydrateEventsDetails(eventMap);
            return new ArrayList<>(eventMap.values());
        } catch (SQLException e) {
            throw translateSqlException("Failed to query events", e);
        }
    }

    private List<Event> queryEventsWithPreloadedClub(String sql, Club club) {
        Map<Integer, Event> eventMap = new LinkedHashMap<>();
        Map<Integer, Club> clubCache = new LinkedHashMap<>();
        clubCache.put(club.getClubId(), club);
        Map<Integer, Category> catCache = new LinkedHashMap<>();

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, club.getClubId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Event event = mapEventRow(rs, clubCache, catCache);
                    eventMap.put(event.getEventId(), event);
                }
            }
            hydrateEventsDetails(eventMap);
            return new ArrayList<>(eventMap.values());
        } catch (SQLException e) {
            throw translateSqlException("Failed to load events for club " + club.getName(), e);
        }
    }

    private void hydrateEventsDetails(Map<Integer, Event> eventsById) throws SQLException {
        if (eventsById.isEmpty()) {
            return;
        }
        Connection conn = db.getConnection();
        Integer[] eventIds = eventsById.keySet().toArray(new Integer[0]);
        Array sqlArray = conn.createArrayOf("int4", eventIds);

        try {
            String regSql = """
                    SELECT r.registration_id, r.event_id, r.registered_at, r.status, r.waitlist_position,
                           u.user_id, u.full_name, u.email, u.password_hash, u.is_active, s.roll_number,
                           att.attendance_id, att.checked_in_at, att.method
                    FROM registrations r
                    JOIN students s ON s.user_id = r.student_id
                    JOIN users u ON u.user_id = s.user_id
                    LEFT JOIN attendances att ON att.registration_id = r.registration_id
                    WHERE r.event_id = ANY(?)
                    ORDER BY r.registration_id ASC
                    """;
            try (PreparedStatement ps = conn.prepareStatement(regSql)) {
                ps.setArray(1, sqlArray);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Event ev = eventsById.get(rs.getInt("event_id"));
                        if (ev == null) {
                            continue;
                        }
                        Student st = new Student(
                                rs.getInt("user_id"),
                                rs.getString("full_name"),
                                rs.getString("email"),
                                rs.getString("password_hash"),
                                rs.getBoolean("is_active"),
                                rs.getString("roll_number")
                        );
                        Attendance att = null;
                        int attId = rs.getInt("attendance_id");
                        if (!rs.wasNull()) {
                            att = new Attendance(
                                    attId,
                                    toLocalDateTime(rs.getTimestamp("checked_in_at")),
                                    CheckInMethod.valueOf(rs.getString("method"))
                            );
                        }
                        Integer waitlistPos = rs.getObject("waitlist_position") != null
                                ? rs.getInt("waitlist_position")
                                : null;
                        Registration reg = new Registration(
                                rs.getInt("registration_id"),
                                st,
                                ev,
                                toLocalDateTime(rs.getTimestamp("registered_at")),
                                RegistrationStatus.valueOf(rs.getString("status")),
                                waitlistPos,
                                att
                        );
                        ev.attachHydratedRegistration(reg);
                    }
                }
            }

            String revSql = """
                    SELECT rv.review_id, rv.event_id, rv.rating, rv.comment, rv.posted_at,
                           u.user_id, u.full_name, u.email, u.password_hash, u.is_active, s.roll_number
                    FROM reviews rv
                    JOIN students s ON s.user_id = rv.student_id
                    JOIN users u ON u.user_id = s.user_id
                    WHERE rv.event_id = ANY(?)
                    ORDER BY rv.posted_at DESC
                    """;
            try (PreparedStatement ps = conn.prepareStatement(revSql)) {
                ps.setArray(1, sqlArray);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Event ev = eventsById.get(rs.getInt("event_id"));
                        if (ev == null) {
                            continue;
                        }
                        Student st = new Student(
                                rs.getInt("user_id"),
                                rs.getString("full_name"),
                                rs.getString("email"),
                                rs.getString("password_hash"),
                                rs.getBoolean("is_active"),
                                rs.getString("roll_number")
                        );
                        Review review = new Review(
                                rs.getInt("review_id"),
                                st,
                                ev,
                                rs.getInt("rating"),
                                rs.getString("comment"),
                                toLocalDateTime(rs.getTimestamp("posted_at"))
                        );
                        ev.attachHydratedReview(review);
                    }
                }
            }
        } finally {
            sqlArray.free();
        }
    }

    private RegistrationConflict hydrateConflictRow(ResultSet rs) throws SQLException {
        int conflictId = rs.getInt("conflict_id");
        int eventId = rs.getInt("event_id");
        ConflictType type = ConflictType.valueOf(rs.getString("type"));
        ConflictStatus status = ConflictStatus.valueOf(rs.getString("status"));
        LocalDateTime detectedAt = toLocalDateTime(rs.getTimestamp("detected_at"));
        String resStr = rs.getString("resolution");
        ResolutionType resolution = resStr != null ? ResolutionType.valueOf(resStr) : null;
        String reason = rs.getString("reason");
        LocalDateTime resolvedAt = toLocalDateTime(rs.getTimestamp("resolved_at"));
        int resolvedById = rs.getInt("resolved_by");
        Admin resolvedBy = !rs.wasNull() ? findAdminById(resolvedById) : null;

        Event event = findEventById(eventId);
        List<Registration> concerned = new ArrayList<>();
        String linkSql = "SELECT registration_id FROM conflict_registrations WHERE conflict_id = ? ORDER BY registration_id ASC";
        try (PreparedStatement ps = db.getConnection().prepareStatement(linkSql)) {
            ps.setInt(1, conflictId);
            try (ResultSet lrs = ps.executeQuery()) {
                while (lrs.next()) {
                    Registration reg = event.findRegistration(lrs.getInt("registration_id"));
                    if (reg != null) {
                        concerned.add(reg);
                    }
                }
            }
        }

        return new RegistrationConflict(
                conflictId, event, type, status, detectedAt,
                resolution, reason, resolvedAt, resolvedBy, concerned
        );
    }

    private Event mapEventRow(ResultSet rs,
                              Map<Integer, Club> clubCache,
                              Map<Integer, Category> catCache) throws SQLException {
        int clubId = rs.getInt("club_id");
        Club club = clubCache.computeIfAbsent(clubId, id -> {
            try {
                return new Club(
                        id,
                        rs.getString("club_name"),
                        rs.getString("club_desc"),
                        rs.getBoolean("club_active"),
                        null
                );
            } catch (SQLException e) {
                throw new DataAccessException("Failed mapping club row", e);
            }
        });

        int catId = rs.getInt("category_id");
        Category category = catCache.computeIfAbsent(catId, id -> {
            try {
                return new Category(id, rs.getString("category_name"));
            } catch (SQLException e) {
                throw new DataAccessException("Failed mapping category row", e);
            }
        });

        return new Event(
                rs.getInt("event_id"),
                club,
                category,
                rs.getString("title"),
                rs.getString("description"),
                toLocalDateTime(rs.getTimestamp("start_date_time")),
                toLocalDateTime(rs.getTimestamp("end_date_time")),
                rs.getString("venue"),
                rs.getInt("capacity"),
                EventStatus.valueOf(rs.getString("status")),
                rs.getString("check_in_code")
        );
    }

    private User mapUserRow(ResultSet rs) throws SQLException {
        int userId = rs.getInt("user_id");
        String fullName = rs.getString("full_name");
        String email = rs.getString("email");
        String passwordHash = rs.getString("password_hash");
        boolean isActive = rs.getBoolean("is_active");
        UserRole role = UserRole.valueOf(rs.getString("role"));

        return switch (role) {
            case STUDENT -> new Student(
                    userId, fullName, email, passwordHash, isActive,
                    rs.getString("roll_number") != null ? rs.getString("roll_number") : "UNASSIGNED"
            );
            case CLUB_ORGANIZER -> new ClubOrganizer(
                    userId, fullName, email, passwordHash, isActive,
                    rs.getBoolean("is_president")
            );
            case ADMIN -> new Admin(
                    userId, fullName, email, passwordHash, isActive,
                    rs.getString("department")
            );
        };
    }

    private static LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }

    private static DataAccessException translateSqlException(String context, SQLException e) {
        return new DataAccessException(context + ": " + e.getMessage(), e);
    }
}
