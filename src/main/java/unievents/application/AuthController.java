package unievents.application;

import unievents.domain.User;
import unievents.infrastructure.persistence.DataHandler;
import unievents.shared.DomainException;

import java.util.List;

/**
 * Thin session controller satisfying the authentication precondition for role-based navigation.
 */
public class AuthController {
    private final DataHandler dataHandler;
    private User currentUser;

    public AuthController(DataHandler dataHandler) {
        this.dataHandler = dataHandler;
    }

    public User login(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new DomainException("Please enter both email and password.");
        }
        User user = dataHandler.findUserByEmail(email.trim());
        if (user == null || !user.verifyPassword(password)) {
            throw new DomainException("Invalid email or password.");
        }
        if (!user.isActive()) {
            throw new DomainException("This account is currently inactive. Please contact Student Affairs.");
        }
        this.currentUser = user;
        return user;
    }

    public List<User> getDemoUsers() {
        return dataHandler.findAllActiveUsers();
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void logout() {
        this.currentUser = null;
    }
}
