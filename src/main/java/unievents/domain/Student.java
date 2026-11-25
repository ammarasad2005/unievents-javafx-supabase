package unievents.domain;

import unievents.shared.DomainException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Student extends User {
    private final String rollNumber;
    private final List<Registration> registrations = new ArrayList<>();

    public Student(int userId, String fullName, String email, String passwordHash,
                   boolean isActive, String rollNumber) {
        super(userId, fullName, email, passwordHash, isActive);
        if (rollNumber == null || rollNumber.isBlank()) {
            throw new DomainException("Student roll number is required.");
        }
        this.rollNumber = rollNumber.trim();
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public List<Registration> getRegistrations() {
        return Collections.unmodifiableList(registrations);
    }

    public void addRegistration(Registration registration) {
        if (registration != null && !registrations.contains(registration)) {
            registrations.add(registration);
        }
    }

    public Registration findRegistration(int registrationId) {
        for (Registration reg : registrations) {
            if (reg.getRegistrationId() == registrationId) {
                return reg;
            }
        }
        return null;
    }

    @Override
    public void receiveNotification(Notification notification) {
        super.receiveNotification(notification);
    }

    @Override
    public String getRole() {
        return "STUDENT";
    }

    @Override
    public String getRoleDisplayName() {
        return "Student";
    }

    @Override
    public String getProfileSummary() {
        return getFullName() + " · Roll #" + rollNumber;
    }

    @Override
    public <T> T accept(UserRoleVisitor<T> visitor) {
        return visitor.visitStudent(this);
    }

    @Override
    public String toString() {
        return getFullName() + " (" + rollNumber + ")";
    }
}
