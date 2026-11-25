package unievents.domain;

import unievents.shared.DomainException;

public class Admin extends User {
    private final String department;

    public Admin(int userId, String fullName, String email, String passwordHash,
                 boolean isActive, String department) {
        super(userId, fullName, email, passwordHash, isActive);
        this.department = department != null ? department.trim() : "";
    }

    public String getDepartment() {
        return department;
    }

    /**
     * UC11 / SD-11: Admin creates a new Club and optionally assigns a ClubOrganizer as President.
     */
    public Club createClub(String name, String description, ClubOrganizer president) {
        if (name == null || name.isBlank()) {
            throw new DomainException("Club name is required.");
        }
        Club club = new Club(0, name.trim(), description != null ? description.trim() : "", true, this);
        if (president != null) {
            club.addOrganizer(president);
            president.assignAsPresident(club);
        }
        return club;
    }

    @Override
    public String getRole() {
        return "ADMIN";
    }

    @Override
    public String getRoleDisplayName() {
        return "Administrator";
    }

    @Override
    public String getProfileSummary() {
        return getFullName() + (department.isEmpty() ? " · Campus Admin" : " · " + department);
    }

    @Override
    public <T> T accept(UserRoleVisitor<T> visitor) {
        return visitor.visitAdmin(this);
    }
}
