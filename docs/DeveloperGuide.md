# Job Application Tracker Developer Guide

This guide describes release **1.0.0**, as declared in `build.gradle` and represented by commit `5ef28c1`. It documents the implementation currently in the repository rather than a proposed architecture.

## Product scope and design goals

Job Application Tracker is a single-user desktop program for managing job applications and their dated tasks. Its release scope is defined by the repository's [`AGENTS.md`](../AGENTS.md): application CRUD, five application statuses, per-application todos, a separate due-date view, JavaFX, and local SQLite persistence.

The implementation favors:

- local, offline persistence with no account or server;
- a small layered design that is easy to inspect and change;
- immutable values at the model boundary;
- transactional writes when an application and its tasks change together; and
- a compact, table-oriented desktop UI.

This is not a multi-user system. It has no authentication, network API, background scheduler, notifications, or synchronization.

## Technology baseline

| Area | Released choice |
| --- | --- |
| Language/toolchain | Java 25 |
| UI | JavaFX Controls 25.0.1; controls are constructed in Java, without FXML |
| Build | Gradle Wrapper 9.7.0 with the Gradle `application` plugin |
| JavaFX build integration | `org.openjfx.javafxplugin` 0.1.0 |
| Database | SQLite through `org.xerial:sqlite-jdbc:3.50.3.0` |
| Logging binding | `org.slf4j:slf4j-nop:2.0.17` at runtime |
| Tests | JUnit Jupiter/BOM 5.13.4 |

The project is a non-modular Java application: there is no `module-info.java`. Only the `javafx.controls` module is requested. The build enables native access for the SQLite driver in both the application and test JVMs.

## Source layout

```text
src/main/java/jobtracker/
├── JobTrackerApp.java             JavaFX entry point and composition root
├── data/
│   ├── DatabaseManager.java       database location, connections, schema setup
│   └── TrackerRepository.java     queries, commands, mapping, transactions
├── model/
│   ├── ApplicationStatus.java     fixed status values and display names
│   ├── JobApplication.java        immutable application record
│   └── TodoItem.java              immutable task record
└── ui/
    ├── MainView.java              navigation and cross-view refresh coordination
    ├── ApplicationPane.java       application table, filters, CRUD actions
    ├── ApplicationDialog.java     application form and staged task list
    ├── TodoDialog.java            task form
    ├── UpcomingTasksPane.java     incomplete task table and completion action
    └── UiUtil.java                formatting, icons, validation, alerts

src/main/resources/styles.css
src/test/java/jobtracker/data/TrackerRepositoryTest.java
```

## Architecture

The system uses a three-layer desktop architecture with a small composition root:

```text
JobTrackerApp
    │ constructs
    ▼
MainView ──► ApplicationPane ──► ApplicationDialog ──► TodoDialog
    │
    └──────► UpcomingTasksPane
                   │
                   ▼
            TrackerRepository
                   │
                   ▼
            DatabaseManager ──► SQLite file

UI, repository, and database mapping exchange JobApplication,
TodoItem, and ApplicationStatus model values.
```

Dependencies point inward from UI to the repository and models. The data package depends on the models, but the models do not depend on JavaFX, JDBC, or the data package. There is no dependency-injection framework: `JobTrackerApp` creates a `DatabaseManager`, wraps it in one `TrackerRepository`, and passes that repository into `MainView`.

### Application entry point

`JobTrackerApp` extends `javafx.application.Application`. Its `start` method creates the repository and main view, loads the CSS resource, configures an 1180 × 760 scene with a 940 × 620 minimum stage size, and shows the window.

The default database path is `${user.home}/.job-application-tracker/tracker.db`. The `jobtracker.database` JVM system property overrides it. `DatabaseManager` creates a missing parent directory before connecting.

### Domain model

`JobApplication` and `TodoItem` are Java records. Their compact constructors normalize nullable strings and defensively copy the task list. This keeps objects immutable after construction and prevents callers from mutating an application's tasks through the original list.

`ApplicationStatus` is an enum with exactly five persisted names:

- `IN_PROGRESS`
- `OFFERED`
- `ACCEPTED`
- `DECLINED`
- `REJECTED`

The enum also owns the user-facing labels, keeping database values stable while allowing readable UI text.

`TodoItem` has convenience constructors that default older date-only calls to `09:00` and omit joined company/job-title values. The full record also carries company and job title when a task is loaded for the cross-application Upcoming tasks view.

### User-interface layer

`MainView` owns the two persistent panes and switches them in a central `StackPane`. It injects the same `refreshAll` callback into both panes. After any successful mutation, that callback reloads both views from SQLite so their counts and rows remain consistent.

`ApplicationPane` maintains an `ObservableList<JobApplication>` backed by a `FilteredList`. Text search and status selection set one combined predicate. The text predicate checks company, job title, and description case-insensitively. The table's default repository order is newest application date and then newest ID; JavaFX column sorting can override the displayed order.

Creating or editing opens `ApplicationDialog`. The dialog copies the application's tasks into a local `ObservableList`. Nested `TodoDialog` operations modify only that staged list. Nothing is persisted until the enclosing application dialog returns a result and the pane calls the repository. Consequently, cancelling the application dialog discards application and task edits together.

`UpcomingTasksPane` loads every incomplete task, including overdue tasks. The repository initially sorts by due date, due time, and company. The UI derives relative timing from `LocalDate.now()`, highlights overdue/today rows with CSS, and delegates the check-mark action to `setTodoCompleted`. Completed tasks disappear on refresh.

All database calls currently run synchronously on the JavaFX application thread. This is simple and acceptable for a small personal database, but a large database or slow storage could briefly freeze the UI. Any future background execution must marshal observable-list and control changes back onto the JavaFX application thread.

### Validation and error handling

The dialogs provide the first validation boundary:

- applications require company, job title, application date, and status;
- tasks require title, due date, and a parseable 24-hour time using `H:mm`;
- description and task type are optional; a blank task type becomes `Other`.

`TrackerRepository` repeats the required-field checks so correctness does not rely only on the UI. SQL uses prepared statements for values. Persistence failures are wrapped in `IllegalStateException` with an operation-level message; the UI unwraps the deepest cause and displays it in a JavaFX error alert.

### Presentation styling and accessibility

The interface is styled through one JavaFX CSS file. It uses a fixed dark sidebar, neutral light content surfaces, compact controls, status colors, and special today/overdue table rows. The design follows the project-local [`UI.md`](../UI.md) direction rather than a separate component framework.

Edit, delete, complete, and refresh controls have accessible text and tooltips. The first three render reusable SVG path data in `SVGPath`; refresh uses the Unicode loop-arrow character.

## Persistence design

### Schema

`DatabaseManager.initialize` creates the schema idempotently with `CREATE TABLE IF NOT EXISTS` and creates two query indexes.

```text
applications                               todos
-----------------------------              --------------------------------
id                INTEGER PK  ◄──────────── application_id INTEGER NOT NULL
company_name      TEXT NOT NULL             id             INTEGER PK
job_title         TEXT NOT NULL             title          TEXT NOT NULL
description       TEXT NOT NULL             type           TEXT NOT NULL
application_date  TEXT NOT NULL             due_date       TEXT NOT NULL
status            TEXT NOT NULL             due_time       TEXT NOT NULL
created_at        TEXT NOT NULL             completed      INTEGER NOT NULL
updated_at        TEXT NOT NULL
```

The relationship is one application to zero or more todos. `todos.application_id` uses `ON DELETE CASCADE`. `DatabaseManager.connect` executes `PRAGMA foreign_keys = ON` for every new connection because SQLite foreign-key enforcement is connection-specific.

Dates and times are stored as ISO-8601 text produced by `LocalDate.toString()` and `LocalTime.toString()`. The status column stores enum constant names and has a `CHECK` constraint. The completed flag is restricted to `0` or `1`.

Indexes:

- `idx_todos_due_date` supports chronological task access;
- `idx_todos_application` supports loading and deleting an application's tasks.

### Repository operations

| Operation | Behavior |
| --- | --- |
| `findAllApplications` | Loads applications by application date/ID descending, then loads each application's tasks by date, time, and ID. |
| `saveApplication` | Inserts the application, obtains its generated ID, inserts all tasks, and commits one transaction. |
| `updateApplication` | Updates the application, deletes its existing tasks, inserts the submitted task snapshot, and commits one transaction. |
| `deleteApplication` | Deletes the application; the foreign key cascades to its tasks. |
| `findUpcomingTodos` | Joins tasks to applications, selects `completed = 0`, and orders by date, time, and company. |
| `setTodoCompleted` | Updates the task's integer completion flag. |

Application save/update is atomic: auto-commit is disabled, and any `SQLException` triggers rollback. Updating deliberately treats the submitted task list as the complete current state. It replaces all stored tasks rather than matching task IDs individually. This simplifies consistency but gives tasks new database IDs after an application edit.

The application-list read uses one task query per application. That N+1 pattern keeps mapping straightforward for the current personal-use scale; it should be replaced with a joined/batched read if the data volume grows substantially.

### Schema evolution

Release 1.0.0 contains one compatibility migration. On startup, `DatabaseManager` checks `PRAGMA table_info(todos)`. If `due_time` is absent, it adds the non-null column with `09:00` as its default. This preserves databases created by the earlier date-only implementation.

There is no numbered migration framework or schema-version table. Future incompatible changes should introduce one before multiple migrations accumulate.

## Build and execution

Use the checked-in wrapper so all developers use Gradle 9.7.0:

```bash
./gradlew clean build
./gradlew run
```

Gradle resolves dependencies from Maven Central. The Java toolchain declaration requires Java 25 for compilation and execution. The `application` plugin sets `jobtracker.JobTrackerApp` as the main class and adds `--enable-native-access=ALL-UNNAMED` for the SQLite native driver.

To test without touching personal data:

```bash
JAVA_TOOL_OPTIONS="-Djobtracker.database=$PWD/development.db" ./gradlew run
```

See [`UserGuide.md`](UserGuide.md) for end-user setup and operation.

## Testing strategy

Run all automated tests with:

```bash
./gradlew test
```

`TrackerRepositoryTest` is an integration test suite for the model/data boundary. JUnit's `@TempDir` gives each test a temporary directory, and `@BeforeEach` creates a fresh SQLite file through the production `DatabaseManager`. The four released tests cover:

1. saving and reloading an application and its task, including due time and joined company data;
2. updating application fields and replacing tasks;
3. hiding completed tasks from the upcoming query and cascading deletion; and
4. upgrading a legacy `todos` table by adding a `09:00` due time.

These tests exercise actual SQL and transactions rather than mocking JDBC. They do not cover JavaFX rendering, dialog interaction, filtering predicates, validation alerts, failure/rollback injection, custom database-path resolution, or concurrent access. Those behaviors currently require the manual test in the user guide. UI automation and additional repository edge cases are the highest-value next test additions.

The HTML report is generated at `build/reports/tests/test/index.html`.

## Software engineering process

### Requirements and design inputs

Development was requirements-led. [`AGENTS.md`](../AGENTS.md) is the functional and technology specification. [`UI.md`](../UI.md) is the local visual-design specification, emphasizing a conventional fixed sidebar, compact forms/tables, calm colors, restrained borders, and minimal decorative styling. The user guide is the behavioral reference for release 1.0.0; code and tests remain authoritative if documentation drifts.

### Iterative delivery reflected in version control

The repository history shows five small release iterations:

1. `a9d8777` established the complete vertical slice: build, models, schema, repository, JavaFX CRUD/task screens, styling, README, and repository tests.
2. `9edf855` revised the presentation against `UI.md`, adding accessible icon utilities and simplifying layout/styling.
3. `0411215` refined application-dialog layout.
4. `00b0324` extended tasks from due dates to due date-times across model, schema, repository, UI, migration, and tests.
5. `5ef28c1` made final table/action presentation adjustments for the current release.

This sequence is feature-slice followed by UI feedback and an end-to-end data-model change. The due-time change demonstrates the expected change discipline: modify the model, persistence/schema migration, both affected views/dialogs, formatting/validation, and automated tests in the same iteration.

The repository does not contain evidence of a pull-request review workflow or continuous-integration configuration, so this guide does not claim either. The locally recorded release gate is a successful `./gradlew test`; it should be supplemented by the manual JavaFX workflow in the user guide.

### Change guidelines

When extending the product:

1. Update the requirement and user-facing behavior documentation.
2. Change immutable model values first.
3. Add an idempotent database migration that preserves existing user data.
4. Keep multi-table writes transactional and use prepared statements.
5. Update every UI that reads or edits the changed data.
6. Add repository integration tests and manually exercise affected JavaFX paths.
7. Run `./gradlew test` and inspect the diff before committing.

Do not silently rename persisted enum constants: existing rows are read with `ApplicationStatus.valueOf`. Either retain the stored name or migrate existing rows. Similarly, do not remove per-connection foreign-key activation, or cascade deletion will no longer be reliable.

## Acknowledgements and reused material

The following sources, tools, assets, and dependencies influenced or are incorporated into release 1.0.0. No external application/tutorial source code was identified in the available repository history beyond the generated/reused material listed here.

### Project-provided ideas and documentation

- [`AGENTS.md`](../AGENTS.md), supplied with the project, provided the product requirements, required technology choices, entity fields, status set, CRUD scope, and separate upcoming-task view.
- [`UI.md`](../UI.md), supplied with the project, provided the visual-design direction used for the sidebar, tables, forms, colors, borders, spacing, and avoidance of decorative dashboard patterns.
- [`logs/conversation-summary.md`](../logs/conversation-summary.md) records the AI-assisted implementation and revision history used when reconstructing the engineering process in this guide.

### AI assistance

- [OpenAI Codex](https://developers.openai.com/) assisted with codebase inspection, implementation and UI revisions recorded by the repository, automated test execution, and drafting/revising the project documentation. Its output was checked against the current source and tests; responsibility for review and inclusion remains with the project author.

### Reused code and assets

- The checked-in `gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.jar` are generated components of the [Gradle Wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html), reused to provision the declared Gradle version consistently. Gradle is licensed under Apache License 2.0.
- The edit, delete, and check SVG path data in `UiUtil` comes from Google's classic [Material Design Icons](https://github.com/google/material-design-icons), reused under Apache License 2.0.

### Libraries and documentation

- [OpenJDK/Java](https://openjdk.org/) supplies the language, records, date/time APIs, collections, file APIs, and JDBC interfaces. OpenJDK licensing is documented under GPLv2 with the Classpath Exception.
- [OpenJFX](https://openjfx.io/) supplies the application lifecycle, scene graph, controls, observable collections, CSS support, and SVG rendering. The project followed the official [JavaFX Gradle guidance](https://openjfx.io/openjfx-docs/) and uses the [OpenJFX Gradle plugin](https://github.com/openjfx/javafx-gradle-plugin). OpenJFX is licensed under GPLv2 with the Classpath Exception.
- [SQLite](https://www.sqlite.org/) supplies the embedded relational database. The schema and connection setup follow the official documentation for [`CREATE TABLE` constraints](https://www.sqlite.org/lang_createtable.html) and [per-connection foreign-key enforcement](https://www.sqlite.org/foreignkeys.html).
- [Xerial SQLite JDBC](https://github.com/xerial/sqlite-jdbc) supplies Java-to-SQLite connectivity and packaged native SQLite libraries. The project uses version 3.50.3.0; its repository identifies Apache-2.0 and BSD-2-Clause licensing.
- [Gradle](https://docs.gradle.org/current/userguide/userguide.html) supplies dependency resolution, the Java toolchain, compilation, execution, test orchestration, and distribution tasks. The build uses its [Application plugin](https://docs.gradle.org/current/userguide/application_plugin.html) and [Java toolchain support](https://docs.gradle.org/current/userguide/toolchains.html).
- [JUnit 5.13.4](https://docs.junit.org/5.13.4/user-guide/) supplies the Jupiter test API, assertions, lifecycle annotations, and `@TempDir`. JUnit 5 is licensed under EPL 2.0.
- [SLF4J NOP 2.0.17](https://www.slf4j.org/faq.html) supplies the no-operation logging provider used at runtime to satisfy logging without emitting application logs. SLF4J is distributed under the [MIT license](https://www.slf4j.org/license.html).

External documentation was paraphrased; it was not copied verbatim into this guide. Dependency declarations in `build.gradle` are the authoritative record of the exact versions shipped by this release.
