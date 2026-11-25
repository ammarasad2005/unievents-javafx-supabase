package unievents.application;

import unievents.domain.Admin;
import unievents.domain.RegistrationConflict;
import unievents.infrastructure.persistence.DataHandler;
import unievents.shared.ConflictDetails;
import unievents.shared.ConflictStatus;
import unievents.shared.ConflictSummary;
import unievents.shared.DomainException;
import unievents.shared.ResolutionType;

import java.util.List;

/**
 * UC12 / SD-12: Admin Inspects and Resolves Open Registration Conflicts.
 */
public class ConflictController {
    private final DataHandler dataHandler;
    private final Admin admin;

    public ConflictController(DataHandler dataHandler, Admin admin) {
        this.dataHandler = dataHandler;
        this.admin = admin;
    }

    public List<ConflictSummary> getOpenConflicts() {
        return dataHandler.findOpenConflicts().stream()
                .map(RegistrationConflict::toSummary)
                .toList();
    }

    public ConflictDetails getConflictDetails(int conflictId) {
        RegistrationConflict conflict = dataHandler.findConflictById(conflictId);
        if (conflict == null) {
            throw new DomainException("Conflict #" + conflictId + " was not found.");
        }
        return conflict.toDetails();
    }

    public ConflictStatus resolveConflict(int conflictId, ResolutionType resolution, String reason) {
        if (admin == null) {
            throw new DomainException("Only an Admin may resolve registration conflicts.");
        }
        RegistrationConflict conflict = dataHandler.findConflictById(conflictId);
        if (conflict == null) {
            throw new DomainException("Conflict #" + conflictId + " was not found.");
        }
        ConflictStatus status = conflict.resolve(admin, resolution, reason);
        dataHandler.saveConflict(conflict);
        return status;
    }
}
