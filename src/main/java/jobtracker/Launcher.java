package jobtracker;

/**
 * Starts the JavaFX application without extending JavaFX Application itself.
 * This is required when launching the packaged executable JAR directly.
 */
public final class Launcher {
    private Launcher() {
    }

    public static void main(String[] args) {
        JobTrackerApp.main(args);
    }
}
