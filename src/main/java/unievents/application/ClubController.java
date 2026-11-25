package unievents.application;

import unievents.domain.Admin;
import unievents.domain.Club;
import unievents.domain.ClubOrganizer;
import unievents.infrastructure.persistence.DataHandler;
import unievents.shared.DomainException;

import java.util.List;

/**
 * UC11 / SD-11: Admin Creates Official Club & Optionally Assigns Club President.
 */
public class ClubController {
    private final DataHandler dataHandler;
    private final Admin admin;

    public ClubController(DataHandler dataHandler, Admin admin) {
        this.dataHandler = dataHandler;
        this.admin = admin;
    }

    public Club createClub(String name, String description, Integer presidentId) {
        if (admin == null) {
            throw new DomainException("Only an Admin may create clubs.");
        }
        ClubOrganizer president = null;
        if (presidentId != null && presidentId > 0) {
            president = dataHandler.findOrganizerById(presidentId);
            if (president == null) {
                throw new DomainException("Selected president organizer (#" + presidentId + ") was not found.");
            }
        }
        Club club = admin.createClub(name, description, president);
        dataHandler.saveClub(club);
        return club;
    }

    public List<ClubOrganizer> getAvailableOrganizers() {
        return dataHandler.findAllOrganizers();
    }

    public List<Club> getAllClubs() {
        return dataHandler.findAllClubs();
    }
}
