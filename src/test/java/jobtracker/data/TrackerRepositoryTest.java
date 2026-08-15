package jobtracker.data;

import jobtracker.model.ApplicationStatus;
import jobtracker.model.JobApplication;
import jobtracker.model.TodoItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
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
        JobApplication saved = repository.saveApplication(application(
                "Aperture Science", "Test Engineer", ApplicationStatus.IN_PROGRESS,
                List.of(new TodoItem(0, 0, "Complete assessment", "Online assessment", dueDate, false))));

        List<JobApplication> applications = repository.findAllApplications();
        assertTrue(saved.id() > 0);
        assertEquals(1, applications.size());
        assertEquals("Aperture Science", applications.getFirst().companyName());
        assertEquals(1, applications.getFirst().todos().size());
        assertEquals(dueDate, repository.findUpcomingTodos().getFirst().dueDate());
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

    private JobApplication application(String company, String title, ApplicationStatus status,
                                       List<TodoItem> todos) {
        return new JobApplication(0, company, title, "Description", LocalDate.now(), status, todos);
    }
}
