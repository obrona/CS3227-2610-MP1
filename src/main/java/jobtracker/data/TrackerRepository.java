package jobtracker.data;

import jobtracker.model.ApplicationStatus;
import jobtracker.model.JobApplication;
import jobtracker.model.TodoItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class TrackerRepository {
    private final DatabaseManager database;

    public TrackerRepository(DatabaseManager database) {
        this.database = database;
    }

    public List<JobApplication> findAllApplications() {
        String sql = "SELECT * FROM applications ORDER BY application_date DESC, id DESC";
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            List<JobApplication> applications = new ArrayList<>();
            while (results.next()) {
                applications.add(mapApplication(connection, results));
            }
            return applications;
        } catch (SQLException exception) {
            throw persistenceFailure("load applications", exception);
        }
    }

    public JobApplication saveApplication(JobApplication application) {
        validate(application);
        String insert = """
                INSERT INTO applications(company_name, job_title, description, application_date, status)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection connection = database.connect()) {
            connection.setAutoCommit(false);
            try {
                long id;
                try (PreparedStatement statement = connection.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
                    bindApplication(statement, application);
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("No application ID was generated");
                        }
                        id = keys.getLong(1);
                    }
                }
                replaceTodos(connection, id, application.todos());
                connection.commit();
                return application.withId(id);
            } catch (SQLException exception) {
                rollback(connection, exception);
                throw exception;
            }
        } catch (SQLException exception) {
            throw persistenceFailure("save application", exception);
        }
    }

    public void updateApplication(JobApplication application) {
        validate(application);
        if (application.id() <= 0) {
            throw new IllegalArgumentException("An existing application must have an ID");
        }
        String update = """
                UPDATE applications
                SET company_name = ?, job_title = ?, description = ?, application_date = ?,
                    status = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """;
        try (Connection connection = database.connect()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(update)) {
                bindApplication(statement, application);
                statement.setLong(6, application.id());
                if (statement.executeUpdate() == 0) {
                    throw new SQLException("Application does not exist: " + application.id());
                }
                replaceTodos(connection, application.id(), application.todos());
                connection.commit();
            } catch (SQLException exception) {
                rollback(connection, exception);
                throw exception;
            }
        } catch (SQLException exception) {
            throw persistenceFailure("update application", exception);
        }
    }

    public void deleteApplication(long id) {
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM applications WHERE id = ?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw persistenceFailure("delete application", exception);
        }
    }

    public List<TodoItem> findUpcomingTodos() {
        String sql = """
                SELECT t.*, a.company_name, a.job_title
                FROM todos t JOIN applications a ON a.id = t.application_id
                WHERE t.completed = 0
                ORDER BY t.due_date ASC, a.company_name COLLATE NOCASE ASC
                """;
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            List<TodoItem> todos = new ArrayList<>();
            while (results.next()) {
                todos.add(mapTodo(results, true));
            }
            return todos;
        } catch (SQLException exception) {
            throw persistenceFailure("load upcoming todos", exception);
        }
    }

    public void setTodoCompleted(long id, boolean completed) {
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE todos SET completed = ? WHERE id = ?")) {
            statement.setInt(1, completed ? 1 : 0);
            statement.setLong(2, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw persistenceFailure("update todo", exception);
        }
    }

    private JobApplication mapApplication(Connection connection, ResultSet results) throws SQLException {
        long id = results.getLong("id");
        return new JobApplication(id,
                results.getString("company_name"),
                results.getString("job_title"),
                results.getString("description"),
                LocalDate.parse(results.getString("application_date")),
                ApplicationStatus.valueOf(results.getString("status")),
                findTodos(connection, id));
    }

    private List<TodoItem> findTodos(Connection connection, long applicationId) throws SQLException {
        String sql = "SELECT * FROM todos WHERE application_id = ? ORDER BY due_date, id";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, applicationId);
            try (ResultSet results = statement.executeQuery()) {
                List<TodoItem> todos = new ArrayList<>();
                while (results.next()) {
                    todos.add(mapTodo(results, false));
                }
                return todos;
            }
        }
    }

    private TodoItem mapTodo(ResultSet results, boolean withApplication) throws SQLException {
        return new TodoItem(results.getLong("id"), results.getLong("application_id"),
                results.getString("title"), results.getString("type"),
                LocalDate.parse(results.getString("due_date")), results.getInt("completed") == 1,
                withApplication ? results.getString("company_name") : "",
                withApplication ? results.getString("job_title") : "");
    }

    private void replaceTodos(Connection connection, long applicationId, List<TodoItem> todos)
            throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM todos WHERE application_id = ?")) {
            delete.setLong(1, applicationId);
            delete.executeUpdate();
        }
        String insert = """
                INSERT INTO todos(application_id, title, type, due_date, completed)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(insert)) {
            for (TodoItem todo : todos) {
                if (todo.title().isBlank() || todo.dueDate() == null) {
                    throw new IllegalArgumentException("Every todo needs a title and due date");
                }
                statement.setLong(1, applicationId);
                statement.setString(2, todo.title());
                statement.setString(3, todo.type());
                statement.setString(4, todo.dueDate().toString());
                statement.setInt(5, todo.completed() ? 1 : 0);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void bindApplication(PreparedStatement statement, JobApplication application)
            throws SQLException {
        statement.setString(1, application.companyName());
        statement.setString(2, application.jobTitle());
        statement.setString(3, application.description());
        statement.setString(4, application.applicationDate().toString());
        statement.setString(5, application.status().name());
    }

    private void validate(JobApplication application) {
        if (application.companyName().isBlank() || application.jobTitle().isBlank()
                || application.applicationDate() == null || application.status() == null) {
            throw new IllegalArgumentException(
                    "Company, job title, application date, and status are required");
        }
        if (application.todos().stream().anyMatch(todo -> todo.title().isBlank()
                || todo.dueDate() == null)) {
            throw new IllegalArgumentException("Every todo needs a title and due date");
        }
    }

    private void rollback(Connection connection, SQLException cause) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            cause.addSuppressed(rollbackFailure);
        }
    }

    private IllegalStateException persistenceFailure(String action, SQLException exception) {
        return new IllegalStateException("Could not " + action, exception);
    }
}
