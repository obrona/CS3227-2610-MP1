package jobtracker.ui;

import jobtracker.data.TrackerRepository;
import jobtracker.model.TodoItem;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

final class UpcomingTasksPane extends BorderPane {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private final TrackerRepository repository;
    private final Runnable changed;
    private final ObservableList<TodoItem> todos = FXCollections.observableArrayList();
    private final TableView<TodoItem> table = new TableView<>(todos);
    private final Label summary = new Label();

    UpcomingTasksPane(TrackerRepository repository, Runnable changed) {
        this.repository = repository;
        this.changed = changed;
        getStyleClass().add("content-pane");
        setPadding(new Insets(24, 30, 30, 30));
        setTop(buildToolbar());
        configureTable();
        setCenter(table);
        BorderPane.setMargin(table, new Insets(18, 0, 0, 0));
    }

    private HBox buildToolbar() {
        summary.getStyleClass().add("task-summary");
        Button refresh = new Button("↻");
        refresh.setAccessibleText("Refresh tasks");
        refresh.setTooltip(new Tooltip("Refresh tasks"));
        refresh.getStyleClass().addAll("icon-button", "refresh-button");
        refresh.setOnAction(event -> refresh());
        HBox toolbar = new HBox(12, summary, new javafx.scene.layout.Region(), refresh);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(toolbar.getChildren().get(1), Priority.ALWAYS);
        return toolbar;
    }

    private void configureTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("You're all caught up — no upcoming tasks."));

        TableColumn<TodoItem, String> due = new TableColumn<>("Due date and time");
        due.setCellValueFactory(value -> new ReadOnlyStringWrapper(
                UiUtil.DATE_FORMAT.format(value.getValue().dueDate()) + " "
                        + TIME_FORMAT.format(value.getValue().dueTime())));
        due.setPrefWidth(185);

        TableColumn<TodoItem, String> timing = new TableColumn<>("When");
        timing.setCellValueFactory(value -> new ReadOnlyStringWrapper(timingText(value.getValue().dueDate())));
        timing.setPrefWidth(120);
        TableColumn<TodoItem, String> task = textColumn("Task", TodoItem::title);
        task.setPrefWidth(220);
        TableColumn<TodoItem, String> type = textColumn("Type", TodoItem::type);
        type.setPrefWidth(155);
        TableColumn<TodoItem, String> company = textColumn("Company", TodoItem::companyName);
        company.setPrefWidth(175);
        TableColumn<TodoItem, String> role = textColumn("Job title", TodoItem::jobTitle);
        role.setPrefWidth(190);

        TableColumn<TodoItem, TodoItem> action = new TableColumn<>("");
        action.setCellValueFactory(value -> new ReadOnlyObjectWrapper<>(value.getValue()));
        action.setCellFactory(column -> new TableCell<>() {
            private final Button done = UiUtil.iconButton(
                    "Mark task as done", UiUtil.CHECK_ICON, "complete-button");
            {
                done.setOnAction(event -> markCompleted(getItem()));
            }
            @Override
            protected void updateItem(TodoItem item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : done);
            }
        });
        action.setPrefWidth(72);
        action.setSortable(false);
        table.getColumns().addAll(List.of(due, timing, task, type, company, role, action));
        table.setRowFactory(view -> new TableRow<>() {
            @Override
            protected void updateItem(TodoItem item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("overdue-row", "today-row");
                if (!empty && item != null) {
                    if (item.dueDate().isBefore(LocalDate.now())) getStyleClass().add("overdue-row");
                    else if (item.dueDate().isEqual(LocalDate.now())) getStyleClass().add("today-row");
                }
            }
        });
    }

    private TableColumn<TodoItem, String> textColumn(
            String title, java.util.function.Function<TodoItem, String> getter) {
        TableColumn<TodoItem, String> column = new TableColumn<>(title);
        column.setCellValueFactory(value -> new ReadOnlyStringWrapper(getter.apply(value.getValue())));
        return column;
    }

    private String timingText(LocalDate dueDate) {
        long days = ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
        if (days < 0) return Math.abs(days) + (days == -1 ? " day overdue" : " days overdue");
        if (days == 0) return "Today";
        if (days == 1) return "Tomorrow";
        return "In " + days + " days";
    }

    void refresh() {
        try {
            todos.setAll(repository.findUpcomingTodos());
            long overdue = todos.stream().filter(todo -> todo.dueDate().isBefore(LocalDate.now())).count();
            summary.setText(todos.size() + (todos.size() == 1 ? " upcoming task" : " upcoming tasks")
                    + (overdue == 0 ? "" : "  •  " + overdue + " overdue"));
        } catch (RuntimeException exception) {
            UiUtil.showError("Tasks could not be loaded", exception);
        }
    }

    private void markCompleted(TodoItem todo) {
        if (todo == null) return;
        try {
            repository.setTodoCompleted(todo.id(), true);
            changed.run();
        } catch (RuntimeException exception) {
            UiUtil.showError("Task could not be completed", exception);
        }
    }
}
