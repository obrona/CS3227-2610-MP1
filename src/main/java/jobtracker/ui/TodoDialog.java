package jobtracker.ui;

import jobtracker.model.TodoItem;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

final class TodoDialog extends Dialog<TodoItem> {
    private final TodoItem existing;
    private final TextField title = new TextField();
    private final ComboBox<String> type = new ComboBox<>();
    private final DatePicker dueDate = new DatePicker(LocalDate.now().plusDays(7));
    private final TextField dueTime = new TextField("09:00");
    private final CheckBox completed = new CheckBox("Completed");

    TodoDialog(TodoItem existing) {
        this.existing = existing;
        setTitle(existing == null ? "Add task" : "Edit task");
        setHeaderText(existing == null ? "Add an upcoming task" : "Update task");
        ButtonType save = new ButtonType(existing == null ? "Add task" : "Save",
                ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, save);
        getDialogPane().setContent(buildForm());
        populate();

        Button saveButton = (Button) getDialogPane().lookupButton(save);
        saveButton.getStyleClass().add("primary-button");
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (title.getText().isBlank() || dueDate.getValue() == null
                    || !UiUtil.isValidTime(dueTime.getText())) {
                event.consume();
                UiUtil.showWarning("A task title, due date, and valid 24-hour time are required.");
            }
        });
        setResultConverter(button -> button == save ? buildResult() : null);
    }

    private GridPane buildForm() {
        title.setPromptText("e.g. Technical interview");
        type.getItems().addAll("Interview", "Online assessment", "Take-home assignment",
                "Follow-up", "Other");
        type.setEditable(true);
        type.getSelectionModel().selectFirst();
        type.setMaxWidth(Double.MAX_VALUE);
        dueDate.setMaxWidth(Double.MAX_VALUE);
        UiUtil.useNumericDate(dueDate);
        dueTime.setPromptText("HH:mm");
        dueTime.setMaxWidth(Double.MAX_VALUE);

        GridPane form = new GridPane();
        form.setPadding(new Insets(4));
        form.setHgap(12);
        form.setVgap(8);
        form.add(label("Task title *"), 0, 0);
        form.add(title, 0, 1, 2, 1);
        form.add(label("Type"), 0, 2);
        form.add(label("Due date *"), 1, 2);
        form.add(type, 0, 3);
        form.add(dueDate, 1, 3);
        form.add(label("Due time * (24-hour)"), 1, 4);
        form.add(dueTime, 1, 5);
        form.add(completed, 0, 5);
        GridPane.setHgrow(title, Priority.ALWAYS);
        GridPane.setHgrow(type, Priority.ALWAYS);
        GridPane.setHgrow(dueDate, Priority.ALWAYS);
        GridPane.setHgrow(dueTime, Priority.ALWAYS);
        form.setPrefWidth(480);
        return form;
    }

    private Label label(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("field-label");
        return label;
    }

    private void populate() {
        if (existing == null) return;
        title.setText(existing.title());
        type.setValue(existing.type());
        dueDate.setValue(existing.dueDate());
        dueTime.setText(DateTimeFormatter.ofPattern("HH:mm").format(existing.dueTime()));
        completed.setSelected(existing.completed());
    }

    private TodoItem buildResult() {
        String selectedType = type.getEditor().getText();
        if (selectedType == null || selectedType.isBlank()) selectedType = "Other";
        return new TodoItem(existing == null ? 0 : existing.id(),
                existing == null ? 0 : existing.applicationId(),
                title.getText(), selectedType, dueDate.getValue(),
                LocalTime.parse(dueTime.getText().strip(), DateTimeFormatter.ofPattern("H:mm")),
                completed.isSelected());
    }
}
