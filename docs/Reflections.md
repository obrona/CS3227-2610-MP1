# Reflections on AI-Assisted Software Engineering

## Introduction

I used an LLM as a software-engineering assistant while building this job application tracker. The assistance covered more than generating Java code: it helped turn requirements into an architecture, implement the JavaFX and SQLite layers, revise the interface, extend the data model, write tests, and produce documentation. The repository history also shows that the application was not produced perfectly in one attempt. It began as a complete vertical slice and was then refined through UI feedback, a task-time feature, minor corrections, and documentation.

This experience showed me that the quality of AI-assisted software engineering depends strongly on the quality and timing of prompts. A prompt is useful not only when it is long and detailed. A short prompt can also be effective when it is given after the LLM already has enough project context. The important factors are whether the prompt defines the goal, supplies relevant constraints, and makes the expected result observable.

The examples below are based on the prompts and prompt-like project instructions recorded in `AGENTS.md`, `UI.md`, `logs/conversation-summary.md`, and the corresponding Git history. Where the repository records only the intent of an iteration rather than its exact original wording, I describe it as a reconstructed prompt instead of presenting it as a quotation.

## Example 1: Defining the whole product through constraints

The first important prompt was the project specification in `AGENTS.md`. Its core request was to build a personal job application tracker in Java 25 with JavaFX, Gradle, and SQLite. It also required CRUD operations, five exact application statuses, todos with dates, and a separate screen that displays upcoming tasks by due date.

This prompt was interesting because it combined three different kinds of requirements:

- product requirements, such as creating, viewing, editing, and deleting applications;
- domain requirements, such as company name, job title, description, application date, status, and todos; and
- technical constraints, such as Java 25, JavaFX, Gradle, and SQLite.

This combination gave the LLM a bounded design space while still leaving room for engineering decisions. For example, the prompt did not prescribe a particular Java package structure or database access pattern. The resulting implementation separated the project into model, data, and UI packages. It used immutable Java records for `JobApplication` and `TodoItem`, an enum for the fixed statuses, a repository for database operations, and separate JavaFX panes for applications and upcoming tasks. These were reasonable design choices inferred from the requirements rather than copied directly from the prompt.

The phrase “separate UI to display upcoming tasks by due date” was especially valuable. Without it, an LLM might have placed every task only inside its parent application dialog. Instead, the requirement encouraged a cross-application query and a dedicated `UpcomingTasksPane`. That decision affected the SQL join, the fields returned with each todo, navigation in `MainView`, and refresh behavior between screens. A short sentence therefore shaped several layers of the system.

At the same time, the prompt left some ambiguities. “Upcoming” could mean only future tasks, or it could include incomplete overdue tasks. The implemented view includes overdue tasks, which is useful but had to be clarified later in the user guide. The original prompt also mentioned task dates but not times, validation rules, cancellation behavior, ordering ties, database migration, or whether completed tasks should remain visible. The LLM filled in these gaps with plausible defaults. This taught me that inferred defaults must be reviewed: reasonable is not the same as explicitly approved.

If I were writing this prompt again, I would include acceptance criteria such as: “Upcoming tasks must show all incomplete tasks, including overdue ones, sorted by due date and time,” and “cancelling an application edit must discard its staged todo changes.” Acceptance criteria would reduce ambiguity and make both implementation and testing more objective.

## Example 2: Using negative constraints to change the visual style

The second interesting prompt was `UI.md`, titled “Uncodixify.” Instead of only describing what the interface should look like, it gave a long list of patterns to avoid: oversized rounded corners, gradients, glass effects, decorative dashboard copy, pill-shaped controls, excessive shadows, fake charts, and other common AI-generated UI conventions. It then paired these prohibitions with positive guidance such as a fixed 240–260 px sidebar, simple borders, compact spacing, ordinary tables, readable typography, and calm colors.

This is an unusual and effective prompting strategy because visual terms such as “modern,” “clean,” or “professional” are too broad. An LLM has seen many interfaces described by those words and may respond with a generic dashboard. The negative examples in `UI.md` narrowed the interpretation. They identified specific failure modes that the user had noticed and made them testable in the generated CSS and layout.

The prompt also showed the value of examples and counterexamples. It included HTML fragments representing forbidden decorative content and concrete measurements for radii, sidebar widths, spacing, and shadows. These details translate more directly into implementation decisions than a subjective request such as “make it less AI-looking.” The UI revision recorded in commit `9edf855` changed `MainView`, the dialogs, both main panes, icon utilities, and much of the stylesheet. The resulting design uses a conventional fixed sidebar, table-oriented content, restrained surfaces, compact controls, and fewer decorative elements.

However, this prompt also demonstrates a risk of very long prompts: internal conflict. `UI.md` discourages blue colors and generic dark SaaS styling, but some of its example palettes contain strong blues and several dark schemes. It says not to use headlines, although ordinary page hierarchy still requires labels and titles. When instructions conflict, the LLM has to guess which rule has priority. In this project, the strongest repeated direction—neutral, compact, functional, and minimally decorative—was more useful than treating every example palette as a requirement.

My main lesson from this prompt is that negative constraints are powerful when a default model tendency is known, but they should be organized by priority. A better version would separate “must not,” “preferred,” and “reference only” rules. It would also define a few visual acceptance checks, for example maximum border radius, sidebar width, allowed accent colors, and whether a page title is required. That would preserve the prompt's strong design intent while reducing contradictions.

## Example 3: Extending “dates” to include task times

A later iteration can be reconstructed from the commit message and implementation as: “Add time to dates for tasks.” This prompt is interesting because it is extremely short, but it describes a domain-model change rather than a cosmetic label change.

A weak response could have added a time text field to the dialog and stopped there. The actual change had to propagate through the entire application:

- `TodoItem` gained a `LocalTime` value;
- the SQLite `todos` table gained a non-null `due_time` column;
- existing databases received a compatibility migration with a `09:00` default;
- repository inserts, updates, reads, and ordering were updated;
- the task dialog gained 24-hour time input and validation;
- application and upcoming-task tables displayed the time;
- upcoming tasks were ordered by date and then time; and
- repository integration tests covered persistence and legacy-schema migration.

This is a good example of why an LLM is useful for change-impact analysis. It can search for every place where a todo date is constructed, stored, formatted, sorted, or tested, then update those locations consistently. The task crossed the domain, persistence, interface, compatibility, and test boundaries. Handling all of them in one iteration reduced the chance of having a UI that appears correct while silently losing the time in SQLite.

The prompt's brevity worked because it came after the project was already built and the LLM could inspect the repository. Context supplied by the code replaced detail that would otherwise have been needed in the prompt. Nevertheless, the LLM still chose policies that were not stated: 24-hour input, `09:00` as the default, minute-level precision, and a migration for existing databases. Those are sensible decisions, but they should be consciously reviewed by the developer.

A stronger prompt would be: “Add a required due time to every todo. Use 24-hour `H:mm` input, default new and migrated tasks to `09:00`, preserve existing databases, sort tasks by date then time, and update repository tests.” This version makes the cross-layer expectations explicit. It also turns hidden assumptions into requirements.

## Example 4: Precise follow-up prompts for UI corrections

The conversation summary records several focused follow-ups: removing page title bars, adding accessible icons for common actions, aligning table headers with row text, padding application forms, and correcting the Upcoming Tasks refresh control to a black loop-arrow icon. These are interesting because they show prompting as an iterative review process rather than a one-shot generation process.

Small visual prompts can be more effective than repeatedly asking the LLM to “improve the UI.” For example, “align table headers with row text” identifies a visible inconsistency and a clear success condition. “Use a black loop-arrow icon for refresh” specifies both the symbol and its color. These requests reduce the chance that an LLM will redesign unrelated parts of the screen while trying to interpret a general aesthetic preference.

They also reveal that correct functionality does not guarantee a finished interface. The initial application already supported the required workflows, but human inspection found spacing, hierarchy, alignment, accessibility, and icon problems. An LLM can implement corrections quickly, yet the developer still needs to look at the rendered interface and express what feels wrong in concrete terms. The most useful feedback names the component, current problem, and desired state.

One limitation is that several small UI changes were committed with broad messages such as “ui changes” or “minor edits.” More descriptive prompts and commit messages would improve traceability. For example, “Align action columns and replace refresh button glyph” makes it easier to connect a request, diff, and design decision later.

## What I learned about LLMs and prompting

The LLM was most valuable as a fast implementation and consistency partner. It could inspect multiple files, recognize architectural relationships, apply a change across layers, and generate supporting tests and documentation. This was particularly useful for repetitive but important work such as SQL mapping, JavaFX table configuration, validation, and migration tests.

Prompting worked best as a sequence:

1. establish the product, domain, and technology constraints;
2. generate a complete but reviewable vertical slice;
3. inspect the running program and tests;
4. give narrow feedback tied to observable behavior; and
5. ask the LLM to verify all affected layers after a change.

I also learned not to treat generated code as automatically correct. LLM output can compile while still making an incorrect product assumption. It may overlook migration of an existing database, update one screen but not another, use a generic visual pattern, or document behavior that the code does not implement. Tests helped verify persistence operations, but they did not test JavaFX rendering or interaction. Manual inspection remained necessary for layout, icons, dialog behavior, and the overall workflow.

The repository is therefore an example of human–AI collaboration rather than autonomous development. The user supplied the goals, technical limits, visual taste, and corrective feedback. The LLM translated those inputs into code and helped propagate changes. The final responsibility remained with the developer: review the diff, run the tests, inspect the UI, identify unstated assumptions, and decide whether the result actually satisfies the intended use.

## Conclusion

This project changed my view of prompting from “telling an AI what code to write” to “providing an evolving engineering specification.” The initial requirements prompt established the system boundaries, the UI prompt controlled an otherwise generic design tendency, the due-time prompt exercised cross-layer change management, and the focused correction prompts improved details that only became clear after inspection.

The strongest prompts were specific enough to constrain the outcome but still allowed the LLM to use the existing codebase as context. The weakest areas were ambiguous terms, conflicting stylistic rules, and undocumented defaults. In future AI-assisted projects, I would keep prompts and acceptance criteria alongside the code, record important assumptions, use descriptive commits, and require both automated verification and human review. That approach uses the speed of an LLM without giving up software-engineering discipline.
