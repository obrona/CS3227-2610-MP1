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
    private final Label pageTitle = new Label();
    private final Label pageSubtitle = new Label();
    private final Button applicationsButton = navigationButton("Applications");
    private final Button tasksButton = navigationButton("Upcoming tasks");

    public MainView(TrackerRepository repository) {
        getStyleClass().add("app-root");
        applicationPane = new ApplicationPane(repository, this::refreshAll);
        tasksPane = new UpcomingTasksPane(repository, this::refreshAll);

        setLeft(buildSidebar());
        setTop(buildHeader());
        setCenter(content);
        showApplications();
    }

    private Node buildSidebar() {
        Label mark = new Label("JT");
        mark.getStyleClass().add("brand-mark");
        Label name = new Label("Job Tracker");
        name.getStyleClass().add("brand-name");
        HBox brand = new HBox(12, mark, name);
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.setPadding(new Insets(2, 8, 26, 8));

        applicationsButton.setOnAction(event -> showApplications());
        tasksButton.setOnAction(event -> showTasks());

        Label hint = new Label("Stay organized.\nLand the right role.");
        hint.getStyleClass().add("sidebar-hint");
        VBox.setMargin(hint, new Insets(0, 8, 6, 8));

        VBox sidebar = new VBox(8, brand, applicationsButton, tasksButton);
        sidebar.getChildren().add(new javafx.scene.layout.Region());
        VBox.setVgrow(sidebar.getChildren().getLast(), javafx.scene.layout.Priority.ALWAYS);
        sidebar.getChildren().add(hint);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(224);
        return sidebar;
    }

    private Node buildHeader() {
        pageTitle.getStyleClass().add("page-title");
        pageSubtitle.getStyleClass().add("page-subtitle");
        VBox titles = new VBox(3, pageTitle, pageSubtitle);
        BorderPane header = new BorderPane(titles);
        header.getStyleClass().add("header");
        return header;
    }

    private Button navigationButton(String text) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.getStyleClass().add("nav-button");
        return button;
    }

    private void showApplications() {
        select(applicationsButton, applicationPane, "Applications",
                "Track every opportunity from application to outcome");
        applicationPane.refresh();
    }

    private void showTasks() {
        select(tasksButton, tasksPane, "Upcoming tasks",
                "Interviews, assessments, and assignments ordered by due date");
        tasksPane.refresh();
    }

    private void select(Button selected, Node view, String title, String subtitle) {
        applicationsButton.getStyleClass().remove("selected");
        tasksButton.getStyleClass().remove("selected");
        selected.getStyleClass().add("selected");
        pageTitle.setText(title);
        pageSubtitle.setText(subtitle);
        content.getChildren().setAll(view);
    }

    private void refreshAll() {
        applicationPane.refresh();
        tasksPane.refresh();
    }
}
