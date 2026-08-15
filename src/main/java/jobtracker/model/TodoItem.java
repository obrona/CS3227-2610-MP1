package jobtracker.model;

import java.time.LocalDate;

public record TodoItem(
        long id,
        long applicationId,
        String title,
        String type,
        LocalDate dueDate,
        boolean completed,
        String companyName,
        String jobTitle
) {
    public TodoItem {
        title = title == null ? "" : title.strip();
        type = type == null ? "Other" : type.strip();
        companyName = companyName == null ? "" : companyName.strip();
        jobTitle = jobTitle == null ? "" : jobTitle.strip();
    }

    public TodoItem(long id, long applicationId, String title, String type,
                    LocalDate dueDate, boolean completed) {
        this(id, applicationId, title, type, dueDate, completed, "", "");
    }
}
