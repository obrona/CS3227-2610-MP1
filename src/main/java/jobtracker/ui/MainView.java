package jobtracker.ui;

import jobtracker.data.TrackerRepository;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public final class MainView extends BorderPane {
    private final ApplicationPane applicationPane;
    private final UpcomingTasksPane tasksPane;
    private final StackPane content = new StackPane();
    private final Button applicationsButton = navigationButton("Applications");
    private final Button tasksButton = navigationButton("Upcoming tasks");

    public MainView(TrackerRepository repository) {
        getStyleClass().add("app-root");
        applicationPane = new ApplicationPane(repository, this::refreshAll);
        tasksPane = new UpcomingTasksPane(repository, this::refreshAll);

        setLeft(buildSidebar());
        setCenter(content);
        showApplications();
    }

    private Node buildSidebar() {
        Label name = new Label("Job Tracker");
        name.getStyleClass().add("brand-name");
        HBox brand = new HBox(name);
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.setPadding(new Insets(0, 10, 24, 10));

        applicationsButton.setOnAction(event -> showApplications());
        tasksButton.setOnAction(event -> showTasks());

        VBox sidebar = new VBox(4, brand, applicationsButton, tasksButton);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(244);
        sidebar.setMinWidth(244);
        return sidebar;
    }

    private Button navigationButton(String text) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.getStyleClass().add("nav-button");
        return button;
    }

    private void showApplications() {
        select(applicationsButton, applicationPane);
        applicationPane.refresh();
    }

    private void showTasks() {
        select(tasksButton, tasksPane);
        tasksPane.refresh();
    }

    private void select(Button selected, Node view) {
        applicationsButton.getStyleClass().remove("selected");
        tasksButton.getStyleClass().remove("selected");
        selected.getStyleClass().add("selected");
        content.getChildren().setAll(view);
    }

    private void refreshAll() {
        applicationPane.refresh();
        tasksPane.refresh();
    }
}
