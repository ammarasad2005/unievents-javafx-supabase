package unievents.infrastructure.persistence;

import unievents.shared.DataAccessException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

/**
 * GoF Singleton managing the PostgreSQL / Supabase JDBC Connection and transaction boundaries.
 * Traces directly to DatabaseController in Deliverable 5 Design Class Diagram.
 */
public final class DatabaseController {
    private static DatabaseController instance;

    private String url;
    private String user;
    private String password;
    private Connection connection;
    private boolean inTransaction = false;

    private DatabaseController() {
        loadConfiguration();
    }

    public static synchronized DatabaseController getInstance() {
        if (instance == null) {
            instance = new DatabaseController();
        }
        return instance;
    }

    private void loadConfiguration() {
        Properties props = new Properties();
        try (InputStream in = DatabaseController.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException ignored) {
            // Fall back to .env / environment / system properties
        }

        // Optional local .env file override
        Path dotenv = Path.of(".env");
        if (Files.isRegularFile(dotenv)) {
            try {
                List<String> lines = Files.readAllLines(dotenv);
                for (String raw : lines) {
                    String line = raw.trim();
                    if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                        continue;
                    }
                    int idx = line.indexOf('=');
                    String k = line.substring(0, idx).trim();
                    String v = line.substring(idx + 1).trim();
                    if ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) {
                        v = v.substring(1, v.length() - 1);
                    }
                    props.setProperty(k, v);
                }
            } catch (IOException ignored) {
            }
        }

        this.url = firstNonBlank(
                System.getProperty("unievents.db.url"),
                System.getenv("UNIEVENTS_DB_URL"),
                props.getProperty("UNIEVENTS_DB_URL"),
                props.getProperty("db.url")
        );
        this.user = firstNonBlank(
                System.getProperty("unievents.db.user"),
                System.getenv("UNIEVENTS_DB_USER"),
                props.getProperty("UNIEVENTS_DB_USER"),
                props.getProperty("db.user")
        );
        this.password = firstNonBlank(
                System.getProperty("unievents.db.password"),
                System.getenv("UNIEVENTS_DB_PASSWORD"),
                props.getProperty("UNIEVENTS_DB_PASSWORD"),
                props.getProperty("db.password")
        );
    }

    private static String firstNonBlank(String... candidates) {
        for (String c : candidates) {
            if (c != null && !c.isBlank()) {
                return c.trim();
            }
        }
        return "";
    }

    /**
     * Allows runtime configuration (e.g. from integration tests or Login DB connection dialog).
     */
    public synchronized void configure(String url, String user, String password) {
        closeConnection();
        this.url = url != null ? url.trim() : "";
        this.user = user != null ? user.trim() : "";
        this.password = password != null ? password : "";
    }

    public synchronized boolean hasPlaceholderCredentials() {
        return url == null || url.isBlank()
                || password == null || password.isBlank()
                || password.contains("<YOUR_SUPABASE_DB_PASSWORD>")
                || password.contains("<DATABASE_PASSWORD>");
    }

    public synchronized String getUrl() { return url; }
    public synchronized String getUser() { return user; }

    public synchronized Connection getConnection() {
        try {
            if (hasPlaceholderCredentials()) {
                throw new DataAccessException(
                        "Database credentials are not configured. Please set UNIEVENTS_DB_PASSWORD in .env or db.password in db.properties.");
            }
            if (connection == null || connection.isClosed()) {
                Class.forName("org.postgresql.Driver");
                DriverManager.setLoginTimeout(8);
                connection = DriverManager.getConnection(url, user, password);
                inTransaction = false;
            }
            return connection;
        } catch (ClassNotFoundException e) {
            throw new DataAccessException("PostgreSQL JDBC Driver not found on classpath.", e);
        } catch (SQLException e) {
            throw new DataAccessException("Database connection failed: " + e.getMessage(), e);
        }
    }

    public synchronized void beginTransaction() {
        try {
            Connection conn = getConnection();
            conn.setAutoCommit(false);
            inTransaction = true;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to begin database transaction: " + e.getMessage(), e);
        }
    }

    public synchronized void commit() {
        if (connection == null) {
            return;
        }
        try {
            connection.commit();
            connection.setAutoCommit(true);
            inTransaction = false;
        } catch (SQLException e) {
            rollback();
            throw new DataAccessException("Failed to commit database transaction: " + e.getMessage(), e);
        }
    }

    public synchronized void rollback() {
        if (connection == null) {
            return;
        }
        try {
            if (!connection.isClosed() && !connection.getAutoCommit()) {
                connection.rollback();
                connection.setAutoCommit(true);
            }
        } catch (SQLException ignored) {
        } finally {
            inTransaction = false;
        }
    }

    public synchronized boolean isInTransaction() {
        return inTransaction;
    }

    public synchronized void closeConnection() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    if (!connection.getAutoCommit()) {
                        connection.rollback();
                    }
                    connection.close();
                }
            } catch (SQLException ignored) {
            } finally {
                connection = null;
                inTransaction = false;
            }
        }
    }
}
