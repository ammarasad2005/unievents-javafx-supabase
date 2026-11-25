package unievents.domain;

/**
 * Polymorphic double-dispatch visitor for User subtypes.
 * Eliminates any need for instanceof or role-string switch statements in UI or controllers.
 */
public interface UserRoleVisitor<T> {
    T visitStudent(Student student);
    T visitOrganizer(ClubOrganizer organizer);
    T visitAdmin(Admin admin);
}
