package jobtracker.model;

import java.time.LocalDate;
import java.time.LocalTime;

public record TodoItem(
        long id,
        long applicationId,
        String title,
        String type,
        LocalDate dueDate,
        LocalTime dueTime,
        boolean completed,
        String companyName,
        String jobTitle
) {
    public TodoItem {
        title = title == null ? "" : title.strip();
        type = type == null ? "Other" : type.strip();
        dueTime = dueTime == null ? LocalTime.of(9, 0) : dueTime;
        companyName = companyName == null ? "" : companyName.strip();
        jobTitle = jobTitle == null ? "" : jobTitle.strip();
    }

    public TodoItem(long id, long applicationId, String title, String type,
                    LocalDate dueDate, boolean completed) {
        this(id, applicationId, title, type, dueDate, LocalTime.of(9, 0), completed, "", "");
    }

    public TodoItem(long id, long applicationId, String title, String type,
                    LocalDate dueDate, LocalTime dueTime, boolean completed) {
        this(id, applicationId, title, type, dueDate, dueTime, completed, "", "");
    }

    public TodoItem(long id, long applicationId, String title, String type,
                    LocalDate dueDate, boolean completed, String companyName, String jobTitle) {
        this(id, applicationId, title, type, dueDate, LocalTime.of(9, 0), completed,
                companyName, jobTitle);
    }
}
