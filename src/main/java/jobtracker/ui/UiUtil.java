package jobtracker.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Tooltip;
import javafx.scene.shape.SVGPath;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

final class UiUtil {
    static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter
            .ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT);
    static final String EDIT_ICON = "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25z M20.71 7.04a.996.996 0 0 0 0-1.41l-2.34-2.34a.996.996 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z";
    static final String DELETE_ICON = "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12z M8 9h8v10H8V9z M15.5 4l-1-1h-5l-1 1H5v2h14V4z";
    static final String CHECK_ICON = "M9 16.17 4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z";

    private UiUtil() {
    }

    static void useNumericDate(DatePicker picker) {
        picker.setPromptText("d/m/yyyy");
        picker.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return date == null ? "" : DATE_FORMAT.format(date);
            }

            @Override
            public LocalDate fromString(String text) {
                if (text == null || text.isBlank()) return null;
                return LocalDate.parse(text.strip(), DATE_FORMAT);
            }
        });
    }

    static boolean isValidTime(String text) {
        if (text == null || text.isBlank()) return false;
        try {
            java.time.LocalTime.parse(text.strip(), java.time.format.DateTimeFormatter.ofPattern("H:mm"));
            return true;
        } catch (DateTimeParseException exception) {
            return false;
        }
    }

    static Button iconButton(String accessibleText, String svgPath, String... styleClasses) {
        SVGPath icon = new SVGPath();
        icon.setContent(svgPath);
        icon.getStyleClass().add("button-icon");

        Button button = new Button();
        button.setGraphic(icon);
        button.setAccessibleText(accessibleText);
        button.setTooltip(new Tooltip(accessibleText));
        button.getStyleClass().add("icon-button");
        button.getStyleClass().addAll(styleClasses);
        return button;
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
