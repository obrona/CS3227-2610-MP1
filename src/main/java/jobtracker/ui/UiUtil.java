package jobtracker.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

final class UiUtil {
    private UiUtil() {
    }

    static void showError(String heading, Throwable error) {
        Alert alert = new Alert(Alert.AlertType.ERROR, readableMessage(error), ButtonType.OK);
        alert.setTitle("Job Tracker");
        alert.setHeaderText(heading);
        alert.showAndWait();
    }

    static void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.setTitle("Job Tracker");
        alert.setHeaderText("Some information is missing");
        alert.showAndWait();
    }

    private static String readableMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? error.getMessage() : current.getMessage();
    }
}
