package jobtracker.ui;

import jobtracker.data.TrackerRepository;
import jobtracker.model.ApplicationStatus;
import jobtracker.model.JobApplication;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

final class ApplicationPane extends BorderPane {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy");
    private final TrackerRepository repository;
    private final Runnable changed;
    private final ObservableList<JobApplication> applications = FXCollections.observableArrayList();
    private final FilteredList<JobApplication> filtered = new FilteredList<>(applications);
    private final TableView<JobApplication> table = new TableView<>(filtered);
    private final TextField search = new TextField();
    private final ComboBox<String> statusFilter = new ComboBox<>();
    private final Label resultCount = new Label();

    ApplicationPane(TrackerRepository repository, Runnable changed) {
        this.repository = repository;
        this.changed = changed;
        getStyleClass().add("content-pane");
        setPadding(new Insets(24, 30, 30, 30));
        configureTable();
        setTop(buildToolbar());
        setCenter(table);
        BorderPane.setMargin(table, new Insets(18, 0, 0, 0));
        configureFilters();
    }

    private HBox buildToolbar() {
        search.setPromptText("Search company or job title");
        search.getStyleClass().add("search-field");
        search.setPrefWidth(300);

        statusFilter.getItems().add("All statuses");
        for (ApplicationStatus status : ApplicationStatus.values()) {
            statusFilter.getItems().add(status.displayName());
        }
        statusFilter.getSelectionModel().selectFirst();

        resultCount.getStyleClass().add("muted-label");
        Button add = new Button("New application");
        add.getStyleClass().add("primary-button");
        add.setOnAction(event -> addApplication());

        HBox tools = new HBox(12, search, statusFilter, resultCount, new javafx.scene.layout.Region(), add);
        tools.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(tools.getChildren().get(3), Priority.ALWAYS);
        return tools;
    }

    private void configureTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No applications yet. Add your first opportunity to get started."));

        TableColumn<JobApplication, String> company = textColumn("Company", JobApplication::companyName);
        company.setPrefWidth(190);
        TableColumn<JobApplication, String> role = textColumn("Job title", JobApplication::jobTitle);
        role.setPrefWidth(230);
        TableColumn<JobApplication, LocalDate> date = new TableColumn<>("Applied");
        date.setCellValueFactory(value -> new ReadOnlyObjectWrapper<>(value.getValue().applicationDate()));
        date.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : DATE_FORMAT.format(item));
            }
        });
        date.setPrefWidth(140);

        TableColumn<JobApplication, ApplicationStatus> status = new TableColumn<>("Status");
        status.setCellValueFactory(value -> new ReadOnlyObjectWrapper<>(value.getValue().status()));
        status.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(ApplicationStatus item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.displayName());
                getStyleClass().removeIf(style -> style.startsWith("status-"));
                if (!empty && item != null) {
                    getStyleClass().add("status-" + item.name().toLowerCase().replace('_', '-'));
                }
            }
        });
        status.setPrefWidth(135);

        TableColumn<JobApplication, Number> tasks = new TableColumn<>("Tasks");
        tasks.setCellValueFactory(value -> new ReadOnlyObjectWrapper<>(
                value.getValue().todos().stream().filter(todo -> !todo.completed()).count()));
        tasks.setPrefWidth(85);

        TableColumn<JobApplication, JobApplication> actions = new TableColumn<>("");
        actions.setCellValueFactory(value -> new ReadOnlyObjectWrapper<>(value.getValue()));
        actions.setCellFactory(column -> new TableCell<>() {
            private final Button edit = UiUtil.iconButton("Edit application", UiUtil.EDIT_ICON);
            private final Button delete = UiUtil.iconButton(
                    "Delete application", UiUtil.DELETE_ICON, "danger-button");
            private final HBox box = new HBox(8, edit, delete);
            {
                edit.setOnAction(event -> editApplication(getItem()));
                delete.setOnAction(event -> deleteApplication(getItem()));
            }
            @Override
            protected void updateItem(JobApplication item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
        actions.setPrefWidth(90);
        actions.setSortable(false);

        table.getColumns().addAll(company, role, date, status, tasks, actions);
        table.setRowFactory(view -> {
            TableRow<JobApplication> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY
                        && event.getClickCount() == 2) {
                    editApplication(row.getItem());
                }
            });
            return row;
        });
    }

    private TableColumn<JobApplication, String> textColumn(
            String title, java.util.function.Function<JobApplication, String> getter) {
        TableColumn<JobApplication, String> column = new TableColumn<>(title);
        column.setCellValueFactory(value -> new ReadOnlyStringWrapper(getter.apply(value.getValue())));
        return column;
    }

    private void configureFilters() {
        search.textProperty().addListener((observable, oldValue, newValue) -> applyFilter());
        statusFilter.valueProperty().addListener((observable, oldValue, newValue) -> applyFilter());
        filtered.addListener((javafx.collections.ListChangeListener<JobApplication>) change -> updateCount());
    }

    private void applyFilter() {
        String query = search.getText() == null ? "" : search.getText().strip().toLowerCase();
        String selectedStatus = statusFilter.getValue();
        filtered.setPredicate(application -> {
            boolean matchesText = query.isEmpty()
                    || application.companyName().toLowerCase().contains(query)
                    || application.jobTitle().toLowerCase().contains(query)
                    || application.description().toLowerCase().contains(query);
            boolean matchesStatus = selectedStatus == null || selectedStatus.equals("All statuses")
                    || application.status().displayName().equals(selectedStatus);
            return matchesText && matchesStatus;
        });
        updateCount();
    }

    private void updateCount() {
        int count = filtered.size();
        resultCount.setText(count + (count == 1 ? " application" : " applications"));
    }

    void refresh() {
        try {
            List<JobApplication> loaded = repository.findAllApplications();
            applications.setAll(loaded);
            applyFilter();
        } catch (RuntimeException exception) {
            UiUtil.showError("Applications could not be loaded", exception);
        }
    }

    private void addApplication() {
        new ApplicationDialog(null).showAndWait().ifPresent(application -> {
            try {
                repository.saveApplication(application);
                changed.run();
            } catch (RuntimeException exception) {
                UiUtil.showError("Application could not be saved", exception);
            }
        });
    }

    private void editApplication(JobApplication application) {
        if (application == null) return;
        new ApplicationDialog(application).showAndWait().ifPresent(updated -> {
            try {
                repository.updateApplication(updated);
                changed.run();
            } catch (RuntimeException exception) {
                UiUtil.showError("Application could not be updated", exception);
            }
        });
    }

    private void deleteApplication(JobApplication application) {
        if (application == null) return;
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete the application for " + application.jobTitle() + " at "
                        + application.companyName() + "? Its tasks will also be deleted.",
                ButtonType.CANCEL, ButtonType.OK);
        confirmation.setTitle("Delete application");
        confirmation.setHeaderText("This action cannot be undone");
        Optional<ButtonType> answer = confirmation.showAndWait();
        if (answer.isPresent() && answer.get() == ButtonType.OK) {
            try {
                repository.deleteApplication(application.id());
                changed.run();
            } catch (RuntimeException exception) {
                UiUtil.showError("Application could not be deleted", exception);
            }
        }
    }
}
