package jobtracker.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseManager {
    private final String jdbcUrl;

    public DatabaseManager() {
        this(resolveDefaultPath());
    }

    public DatabaseManager(Path databasePath) {
        try {
            Path absolutePath = databasePath.toAbsolutePath();
            Path parent = absolutePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            jdbcUrl = "jdbc:sqlite:" + absolutePath;
            initialize();
        } catch (IOException | SQLException exception) {
            throw new IllegalStateException("Could not initialize the database", exception);
        }
    }

    private static Path resolveDefaultPath() {
        String configured = System.getProperty("jobtracker.database");
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured);
        }
        return Path.of(System.getProperty("user.home"), ".job-application-tracker", "tracker.db");
    }

    public Connection connect() throws SQLException {
        Connection connection = DriverManager.getConnection(jdbcUrl);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    private void initialize() throws SQLException {
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS applications (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        company_name TEXT NOT NULL,
                        job_title TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        application_date TEXT NOT NULL,
                        status TEXT NOT NULL CHECK(status IN
                            ('IN_PROGRESS', 'OFFERED', 'ACCEPTED', 'DECLINED', 'REJECTED')),
                        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS todos (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        application_id INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        type TEXT NOT NULL,
                        due_date TEXT NOT NULL,
                        completed INTEGER NOT NULL DEFAULT 0 CHECK(completed IN (0, 1)),
                        FOREIGN KEY(application_id) REFERENCES applications(id) ON DELETE CASCADE
                    )
                    """);
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_todos_due_date ON todos(due_date)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_todos_application ON todos(application_id)");
        }
    }
}
