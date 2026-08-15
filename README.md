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

Applications can be created, searched, filtered, edited, and deleted. Todos are managed inside the application editor, and incomplete items appear by due date on the Upcoming Tasks screen.
