package jobtracker;

import jobtracker.data.DatabaseManager;
import jobtracker.data.TrackerRepository;
import jobtracker.ui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public final class JobTrackerApp extends Application {
    @Override
    public void start(Stage stage) {
        TrackerRepository repository = new TrackerRepository(new DatabaseManager());
        Scene scene = new Scene(new MainView(repository), 1180, 760);
        scene.getStylesheets().add(Objects.requireNonNull(
                getClass().getResource("/styles.css")).toExternalForm());

        stage.setTitle("Job Application Tracker");
        stage.setMinWidth(940);
        stage.setMinHeight(620);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
