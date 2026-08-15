package jobtracker.ui;

import jobtracker.model.ApplicationStatus;
import jobtracker.model.JobApplication;
import jobtracker.model.TodoItem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

final class ApplicationDialog extends Dialog<JobApplication> {
    private final JobApplication existing;
    private final TextField company = new TextField();
    private final TextField jobTitle = new TextField();
    private final TextArea description = new TextArea();
    private final DatePicker applicationDate = new DatePicker(LocalDate.now());
    private final ComboBox<ApplicationStatus> status = new ComboBox<>();
    private final ObservableList<TodoItem> todos = FXCollections.observableArrayList();
    private final TableView<TodoItem> todoTable = new TableView<>(todos);

    ApplicationDialog(JobApplication existing) {
        this.existing = existing;
        setTitle(existing == null ? "New application" : "Edit application");
        setHeaderText(existing == null ? "Add a job application" : "Update job application");
        getDialogPane().getStyleClass().add("application-dialog");
        getDialogPane().setPrefWidth(760);
        getDialogPane().setPrefHeight(700);

        ButtonType save = new ButtonType(existing == null ? "Add application" : "Save changes",
                ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, save);
        getDialogPane().setContent(buildContent());
        populate();

        Button saveButton = (Button) getDialogPane().lookupButton(save);
        saveButton.getStyleClass().add("primary-button");
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (!isValid()) {
                event.consume();
                UiUtil.showWarning("Company name, job title, application date, and status are required. "
                        + "Every task also needs a title, due date, and due time.");
            }
        });
        setResultConverter(button -> button == save ? buildResult() : null);
    }

    private VBox buildContent() {
        company.setPromptText("e.g. Acme Labs");
        jobTitle.setPromptText("e.g. Software Engineer");
        description.setPromptText("Responsibilities, requirements, notes, or a link to the listing");
        description.setWrapText(true);
        description.setPrefRowCount(4);
        status.getItems().setAll(ApplicationStatus.values());
        status.getSelectionModel().select(ApplicationStatus.IN_PROGRESS);
        UiUtil.useNumericDate(applicationDate);

        GridPane form = new GridPane();
        form.setHgap(14);
        form.setVgap(10);
        ColumnConstraints leftColumn = new ColumnConstraints();
        leftColumn.setPercentWidth(50);
        ColumnConstraints rightColumn = new ColumnConstraints();
        rightColumn.setPercentWidth(50);
        form.getColumnConstraints().addAll(leftColumn, rightColumn);
        form.add(label("Company *"), 0, 0);
        form.add(label("Job title *"), 1, 0);
        form.add(company, 0, 1);
        form.add(jobTitle, 1, 1);
        form.add(label("Application date *"), 0, 2);
        form.add(label("Status *"), 1, 2);
        form.add(applicationDate, 0, 3);
        form.add(status, 1, 3);
        form.add(label("Job description"), 0, 4, 2, 1);
        form.add(description, 0, 5, 2, 1);
        GridPane.setHgrow(company, Priority.ALWAYS);
        GridPane.setHgrow(jobTitle, Priority.ALWAYS);
        company.setMaxWidth(Double.MAX_VALUE);
        jobTitle.setMaxWidth(Double.MAX_VALUE);
        applicationDate.setMaxWidth(Double.MAX_VALUE);
        status.setMaxWidth(Double.MAX_VALUE);

        Label tasksTitle = new Label("Tasks");
        tasksTitle.getStyleClass().add("section-title");
        Label tasksHint = new Label("Add interviews, assessments, or take-home assignments.");
        tasksHint.getStyleClass().add("muted-label");
        VBox taskHeading = new VBox(2, tasksTitle, tasksHint);

        Button addTask = new Button("Add task");
        addTask.getStyleClass().add("small-button");
        addTask.setOnAction(event -> addTodo());
        HBox heading = new HBox(taskHeading, new javafx.scene.layout.Region(), addTask);
        heading.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(heading.getChildren().get(1), Priority.ALWAYS);

        configureTodoTable();
        VBox root = new VBox(18, form, heading, todoTable);
        root.setPadding(new Insets(4, 24, 0, 24));
        VBox.setVgrow(todoTable, Priority.ALWAYS);
        return root;
    }

    private Label label(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("field-label");
        return label;
    }

    private void configureTodoTable() {
        todoTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        todoTable.setPlaceholder(new Label("No tasks for this application"));
        todoTable.setPrefHeight(210);

        TableColumn<TodoItem, String> title = new TableColumn<>("Task");
        title.setCellValueFactory(value -> new javafx.beans.property.ReadOnlyStringWrapper(value.getValue().title()));
        title.setPrefWidth(230);
        TableColumn<TodoItem, String> type = new TableColumn<>("Type");
        type.setCellValueFactory(value -> new javafx.beans.property.ReadOnlyStringWrapper(value.getValue().type()));
        type.setPrefWidth(150);
        TableColumn<TodoItem, String> due = new TableColumn<>("Due date and time");
        due.setCellValueFactory(value -> new javafx.beans.property.ReadOnlyStringWrapper(
                UiUtil.DATE_FORMAT.format(value.getValue().dueDate()) + " "
                        + DateTimeFormatter.ofPattern("HH:mm").format(value.getValue().dueTime())));
        due.setPrefWidth(165);
        TableColumn<TodoItem, String> complete = new TableColumn<>("Done");
        complete.setCellValueFactory(value -> new javafx.beans.property.ReadOnlyStringWrapper(
                value.getValue().completed() ? "Yes" : "No"));
        complete.setPrefWidth(65);
        TableColumn<TodoItem, TodoItem> actions = new TableColumn<>("");
        actions.setCellValueFactory(value -> new javafx.beans.property.ReadOnlyObjectWrapper<>(value.getValue()));
        actions.setCellFactory(column -> new javafx.scene.control.TableCell<>() {
            private final Button edit = UiUtil.iconButton("Edit task", UiUtil.EDIT_ICON);
            private final Button remove = UiUtil.iconButton(
                    "Remove task", UiUtil.DELETE_ICON, "danger-button");
            private final HBox buttons = new HBox(6, edit, remove);
            {
                edit.setOnAction(event -> editTodo(getItem()));
                remove.setOnAction(event -> todos.remove(getItem()));
            }
            @Override
            protected void updateItem(TodoItem item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttons);
            }
        });
        actions.setPrefWidth(84);
        actions.setSortable(false);
        todoTable.getColumns().addAll(List.of(title, type, due, complete, actions));
        todoTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && todoTable.getSelectionModel().getSelectedItem() != null) {
                editTodo(todoTable.getSelectionModel().getSelectedItem());
            }
        });
    }

    private void populate() {
        if (existing == null) return;
        company.setText(existing.companyName());
        jobTitle.setText(existing.jobTitle());
        description.setText(existing.description());
        applicationDate.setValue(existing.applicationDate());
        status.setValue(existing.status());
        todos.setAll(existing.todos());
    }

    private void addTodo() {
        new TodoDialog(null).showAndWait().ifPresent(todos::add);
    }

    private void editTodo(TodoItem todo) {
        if (todo == null) return;
        new TodoDialog(todo).showAndWait().ifPresent(updated -> {
            int index = todos.indexOf(todo);
            if (index >= 0) todos.set(index, updated);
        });
    }

    private boolean isValid() {
        return !company.getText().isBlank() && !jobTitle.getText().isBlank()
                && applicationDate.getValue() != null && status.getValue() != null
                && todos.stream().allMatch(todo -> !todo.title().isBlank()
                        && todo.dueDate() != null && todo.dueTime() != null);
    }

    private JobApplication buildResult() {
        return new JobApplication(existing == null ? 0 : existing.id(), company.getText(),
                jobTitle.getText(), description.getText(), applicationDate.getValue(),
                status.getValue(), todos);
    }
}
