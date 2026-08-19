# Job Application Tracker

A personal desktop application for tracking job applications and their upcoming tasks. Built with Java 25, JavaFX, Gradle, and SQLite.

## Run

```bash
./gradlew run
```

The database is created automatically at `~/.job-application-tracker/tracker.db`. To use another location:

```bash
./gradlew run -Djobtracker.database=/path/to/tracker.db
```

## Test

```bash
./gradlew test
```

## Build a runnable JAR

```bash
./gradlew fatJar
java --enable-native-access=ALL-UNNAMED \
    -jar build/libs/job-application-tracker-1.0.0-all.jar
```

The JAR includes the JavaFX, SQLite JDBC, and logging runtime dependencies. It
is platform-specific because JavaFX contains native Linux libraries; rebuild it
on the target operating system when distributing it to another platform.

Applications can be created, searched, filtered, edited, and deleted. Todos are managed inside the application editor, and incomplete items appear by due date on the Upcoming Tasks screen.
