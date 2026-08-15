package jobtracker.data;

import jobtracker.model.ApplicationStatus;
import jobtracker.model.JobApplication;
import jobtracker.model.TodoItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrackerRepositoryTest {
    @TempDir
    Path tempDirectory;

    private TrackerRepository repository;

    @BeforeEach
    void setUp() {
        repository = new TrackerRepository(new DatabaseManager(tempDirectory.resolve("test.db")));
    }

    @Test
    void savesApplicationAndItsTodos() {
        LocalDate dueDate = LocalDate.now().plusDays(3);
        LocalTime dueTime = LocalTime.of(14, 30);
        JobApplication saved = repository.saveApplication(application(
                "Aperture Science", "Test Engineer", ApplicationStatus.IN_PROGRESS,
                List.of(new TodoItem(0, 0, "Complete assessment", "Online assessment",
                        dueDate, dueTime, false))));

        List<JobApplication> applications = repository.findAllApplications();
        assertTrue(saved.id() > 0);
        assertEquals(1, applications.size());
        assertEquals("Aperture Science", applications.getFirst().companyName());
        assertEquals(1, applications.getFirst().todos().size());
        assertEquals(dueDate, repository.findUpcomingTodos().getFirst().dueDate());
        assertEquals(dueTime, repository.findUpcomingTodos().getFirst().dueTime());
        assertEquals("Aperture Science", repository.findUpcomingTodos().getFirst().companyName());
    }

    @Test
    void updatesApplicationAndReplacesTodosAtomically() {
        JobApplication saved = repository.saveApplication(application(
                "Old Company", "Developer", ApplicationStatus.IN_PROGRESS, List.of()));
        TodoItem interview = new TodoItem(0, saved.id(), "Meet the team", "Interview",
                LocalDate.now().plusDays(2), false);
        JobApplication updated = new JobApplication(saved.id(), "New Company", "Senior Developer",
                "Updated details", LocalDate.now(), ApplicationStatus.OFFERED, List.of(interview));

        repository.updateApplication(updated);

        JobApplication loaded = repository.findAllApplications().getFirst();
        assertEquals("New Company", loaded.companyName());
        assertEquals(ApplicationStatus.OFFERED, loaded.status());
        assertEquals(List.of("Meet the team"), loaded.todos().stream().map(TodoItem::title).toList());
    }

    @Test
    void completionHidesTodoFromUpcomingAndDeleteCascades() {
        JobApplication saved = repository.saveApplication(application(
                "Black Mesa", "Researcher", ApplicationStatus.ACCEPTED,
                List.of(new TodoItem(0, 0, "Sign paperwork", "Follow-up",
                        LocalDate.now().plusDays(1), false))));
        TodoItem todo = repository.findUpcomingTodos().getFirst();

        repository.setTodoCompleted(todo.id(), true);
        assertTrue(repository.findUpcomingTodos().isEmpty());

        repository.deleteApplication(saved.id());
        assertTrue(repository.findAllApplications().isEmpty());
        assertTrue(repository.findUpcomingTodos().isEmpty());
    }

    @Test
    void migratesExistingTodosWithANineAmDefaultTime() throws Exception {
        Path oldDatabase = tempDirectory.resolve("old.db");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + oldDatabase);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE applications (
                        id INTEGER PRIMARY KEY, company_name TEXT NOT NULL, job_title TEXT NOT NULL,
                        description TEXT NOT NULL, application_date TEXT NOT NULL, status TEXT NOT NULL,
                        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)
                    """);
            statement.executeUpdate("""
                    CREATE TABLE todos (
                        id INTEGER PRIMARY KEY, application_id INTEGER NOT NULL, title TEXT NOT NULL,
                        type TEXT NOT NULL, due_date TEXT NOT NULL, completed INTEGER NOT NULL DEFAULT 0)
                    """);
            statement.executeUpdate("""
                    INSERT INTO applications(id, company_name, job_title, description, application_date, status)
                    VALUES (1, 'Acme', 'Engineer', '', '2026-08-01', 'IN_PROGRESS')
                    """);
            statement.executeUpdate("""
                    INSERT INTO todos(id, application_id, title, type, due_date, completed)
                    VALUES (1, 1, 'Interview', 'Interview', '2026-08-20', 0)
                    """);
        }

        TrackerRepository migrated = new TrackerRepository(new DatabaseManager(oldDatabase));

        assertEquals(LocalTime.of(9, 0), migrated.findUpcomingTodos().getFirst().dueTime());
    }

    private JobApplication application(String company, String title, ApplicationStatus status,
                                       List<TodoItem> todos) {
        return new JobApplication(0, company, title, "Description", LocalDate.now(), status, todos);
    }
}
