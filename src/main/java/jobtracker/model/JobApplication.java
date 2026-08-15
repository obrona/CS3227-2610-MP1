package jobtracker.model;

import java.time.LocalDate;
import java.util.List;

public record JobApplication(
        long id,
        String companyName,
        String jobTitle,
        String description,
        LocalDate applicationDate,
        ApplicationStatus status,
        List<TodoItem> todos
) {
    public JobApplication {
        companyName = companyName == null ? "" : companyName.strip();
        jobTitle = jobTitle == null ? "" : jobTitle.strip();
        description = description == null ? "" : description.strip();
        todos = todos == null ? List.of() : List.copyOf(todos);
    }

    public JobApplication withId(long newId) {
        return new JobApplication(newId, companyName, jobTitle, description,
                applicationDate, status, todos);
    }
}
