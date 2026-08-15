# Job Application Tracker User Guide

Job Application Tracker is a personal desktop application for storing job applications and the tasks associated with them. It runs locally, uses a JavaFX interface, and saves its data in a SQLite database on the same computer.

## Requirements

- A Linux desktop environment capable of displaying JavaFX applications
- Java Development Kit (JDK) 25
- An internet connection for the first build, so the Gradle wrapper can download Gradle 9.7.0 and the project dependencies

You do not need to install Gradle or SQLite separately. The repository includes the Gradle wrapper, and the SQLite JDBC driver is downloaded as a project dependency.

Check the installed Java version from the project root:

```bash
java -version
```

The output should report Java 25.

## Set up and run the application

1. Open a terminal in the project root (the directory containing `build.gradle` and `gradlew`).
2. If the Gradle wrapper is not executable, run:

   ```bash
   chmod +x gradlew
   ```

3. Start the application:

   ```bash
   ./gradlew run
   ```

On its first run, the application creates the database and its tables automatically. By default, data is stored at:

```text
~/.job-application-tracker/tracker.db
```

The application reuses this database on later runs, so saved applications remain available.

### Use a different database file

The application recognizes the `jobtracker.database` Java system property. For example, this command starts it with a database named `demo.db` in the current directory:

```bash
JAVA_TOOL_OPTIONS="-Djobtracker.database=$PWD/demo.db" ./gradlew run
```

The parent directory and database file are created automatically if necessary. Use a separate database when demonstrating or manually testing the application without changing your normal data.

## Application layout

The left sidebar has two screens:

- **Applications** manages job applications and their tasks.
- **Upcoming tasks** shows all incomplete tasks across applications in due-date order.

The application opens on the **Applications** screen. Switching screens reloads their data from the database.

## Manage applications

### View applications

The Applications table displays each application's company, job title, application date, status, and number of incomplete tasks. Applications initially appear with the most recent application date first. Click a column heading to use JavaFX table sorting for that column.

The task count includes incomplete tasks only.

### Search and filter

Use the search field to match text in the company name, job title, or job description. Matching is case-insensitive and updates as you type.

Use the status list to show all applications or only applications with one of these statuses:

- In progress
- Offered
- Accepted
- Declined
- Rejected

Search text and the status filter apply together. The label beside the filters reports the number of matching applications.

### Create an application

1. Select **Applications** in the sidebar.
2. Click **New application**.
3. Enter the application details.
4. Optionally add tasks as described in [Manage tasks within an application](#manage-tasks-within-an-application).
5. Click **Add application**.

Company name, job title, application date, and status are required. Job description is optional. A new application defaults to today's date and the **In progress** status.

Dates are displayed and accepted as `day/month/year`, for example `15/8/2026`. You can also select a date with the date picker's calendar.

If required information is missing, the application displays a warning and keeps the editor open.

### Edit an application

Open the editor in either of these ways:

- Click the pencil button in the application's row.
- Double-click the application row with the primary mouse button.

Change the application details or its tasks, then click **Save changes**. Clicking **Cancel** closes the editor without saving any changes made during that editing session.

### Delete an application

1. Click the trash button in the application's row.
2. Review the confirmation dialog.
3. Click **OK** to delete, or **Cancel** to keep the application.

Deleting an application also permanently deletes all of its tasks. The confirmation dialog notes that this cannot be undone.

## Manage tasks within an application

Tasks are created, edited, and removed from the new/edit application dialog. They are not committed to the database until you click **Add application** or **Save changes** on the enclosing application dialog.

### Add a task

1. Open a new or existing application.
2. Click **Add task**.
3. Enter a task title.
4. Select or type a task type.
5. Choose a due date and enter a due time.
6. Optionally select **Completed**.
7. Click **Add task**.
8. Save the enclosing application.

The available task type suggestions are **Interview**, **Online assessment**, **Take-home assignment**, **Follow-up**, and **Other**. The field is editable, so you may enter a custom type. If the type is empty, it is saved as **Other**.

Task title, due date, and due time are required. Time must use 24-hour `hour:minute` format, such as `9:00` or `14:30`. A new task defaults to seven days from today at `09:00` and is not completed.

### Edit or remove a task

In the application editor:

- Click a task's pencil button, or double-click the task, to edit its title, type, due date, due time, or completion state.
- Click its trash button to remove it from the application editor.

Click **Save changes** on the enclosing application to persist these changes. Click **Cancel** on the enclosing application to discard all task additions, edits, and removals from that editing session.

## Use the Upcoming tasks screen

Select **Upcoming tasks** in the sidebar to see every incomplete task from every application. Despite the screen's name, the list includes overdue incomplete tasks as well as tasks due today or later. Completed tasks are excluded.

Tasks are initially ordered by due date, due time, and then company name. The table shows:

- Due date and time
- Relative timing: **Today**, **Tomorrow**, **In _n_ days**, or **_n_ day(s) overdue**
- Task title and type
- Related company and job title

Click a column heading to sort the table by that column.

Rows due today and overdue rows receive distinct visual highlighting. The summary above the table reports the total number of incomplete tasks and, when applicable, the overdue count.

Click the check-mark button in a row to mark that task completed. It immediately disappears from Upcoming tasks and the incomplete task count on the Applications screen decreases. To mark it incomplete again, open its application, edit the task, clear **Completed**, and save the application.

Use the refresh button at the upper right of this screen to reload tasks from the database.

## Automated tests

Run the test suite from the project root:

```bash
./gradlew test
```

A successful run ends with `BUILD SUCCESSFUL`. The automated tests use temporary SQLite databases; they do not modify the normal database in `~/.job-application-tracker`.

The current tests verify that:

- an application and its tasks can be saved and loaded, including task due times and company information;
- an application and its tasks can be updated;
- completing a task removes it from the upcoming-task query;
- deleting an application also deletes its tasks; and
- an older database without task due times is upgraded with a default time of `09:00`.

The generated HTML test report is available after the run at:

```text
build/reports/tests/test/index.html
```

## Suggested manual test

Use a separate database so the test does not affect personal records:

```bash
JAVA_TOOL_OPTIONS="-Djobtracker.database=$PWD/manual-test.db" ./gradlew run
```

Then verify this end-to-end workflow:

1. Add an application with all required fields and at least two tasks with different dates or times.
2. Confirm that it appears in Applications and that its task count includes only incomplete tasks.
3. Search using text from its description, then combine the search with its status filter.
4. Edit the application, change its status, edit one task, and save.
5. Open Upcoming tasks and confirm that incomplete tasks are ordered by due date and time.
6. Mark one task done and confirm that it disappears and the application's task count decreases.
7. Reopen the application, edit that task, clear **Completed**, and save; confirm that it returns to Upcoming tasks.
8. Delete the application, confirm the warning, and verify that both the application and its tasks disappear.
9. Close and restart the application with the same command during the workflow to confirm that saved data persists.

## Troubleshooting

- **The build reports an incompatible Java version:** make sure `java -version` reports Java 25 and that `JAVA_HOME` points to that JDK if your machine has multiple JDKs.
- **The first build cannot download Gradle or dependencies:** check the internet connection and proxy settings, then retry `./gradlew run`.
- **The JavaFX window does not appear:** run the program from a graphical desktop session with a working display, rather than a headless terminal.
- **The application reports that data could not be loaded or saved:** check that the database's parent directory is writable and that the database file is not read-only.
- **You started with a custom database but see unexpected data:** use the exact same `JAVA_TOOL_OPTIONS` command each time; otherwise the application falls back to the default database.
