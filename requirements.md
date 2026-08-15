# Requirements Document

## Introduction

The Todo Calendar Grid is a personal task-tracking feature for a JavaFX desktop application. It replaces the placeholder UI with a calendar-grid view where tasks are displayed as rows and each day of the current month occupies a column. A checkbox at each cell indicates whether a task was completed on that day. Only the column for today's date is interactive — all other columns are read-only. The app uses SQLite for persistent storage and follows a dark, minimal aesthetic.

## Glossary

- **App**: The JavaFX Todo desktop application.
- **Task**: A named to-do item created by the user and persisted in the database.
- **CalendarGrid**: The table-style UI component where rows are tasks and columns are days of the current month.
- **Cell**: The intersection of a task row and a date column in the CalendarGrid; contains a single checkbox.
- **CompletionRecord**: A database entry that records whether a specific Task was completed on a specific date.
- **CurrentDate**: The system date at application launch, used to identify the active interactive column.
- **Database**: The SQLite database file (`todo.db`) used for persistent storage.
- **DayColumn**: A column in the CalendarGrid representing a single calendar day within the current month.
- **TaskNameField**: The text input field used to enter the name of a new task.

---

## Requirements

### Requirement 1: Calendar Grid Display

**User Story:** As a user, I want to see all my tasks laid out against a calendar grid for the current month, so that I can track daily completion at a glance.

#### Acceptance Criteria

1. WHEN the App starts, THE CalendarGrid SHALL display one row per Task stored in the Database.
2. WHEN the App starts, THE CalendarGrid SHALL display one DayColumn for each day of the current calendar month, labelled with the day number.
3. THE CalendarGrid SHALL display the task name in the first column of each row.
4. WHEN the App starts, THE CalendarGrid SHALL scroll horizontally so that the CurrentDate column is visible without manual scrolling.

---

### Requirement 2: Checkbox Completion Cells

**User Story:** As a user, I want a checkbox in each cell of the grid, so that I can see and record whether I completed a task on a given day.

#### Acceptance Criteria

1. THE CalendarGrid SHALL render a checkbox inside every Cell.
2. WHEN the App starts, THE CalendarGrid SHALL display each checkbox in a checked state if a CompletionRecord exists for that Task and date with status completed, and in an unchecked state otherwise.
3. WHEN a user toggles a checkbox in the CurrentDate column, THE App SHALL create or update the CompletionRecord for that Task and the CurrentDate in the Database.

---

### Requirement 3: Read-Only Enforcement for Non-Current Dates

**User Story:** As a user, I want only today's column to be interactive, so that I cannot accidentally alter historical or future records.

#### Acceptance Criteria

1. WHILE the DayColumn does not correspond to the CurrentDate, THE CalendarGrid SHALL render all checkboxes in that column as disabled (non-interactive).
2. THE CalendarGrid SHALL render checkboxes in the CurrentDate column as enabled (interactive).
3. IF a user attempts to interact with a disabled checkbox, THEN THE App SHALL ignore the interaction and leave the CompletionRecord unchanged.

---

### Requirement 4: Add Task

**User Story:** As a user, I want to add new tasks by name, so that I can grow my task list over time.

#### Acceptance Criteria

1. THE App SHALL display a TaskNameField and an add button visible at all times.
2. WHEN a user enters a non-whitespace task name in the TaskNameField and activates the add button, THE App SHALL persist the new Task to the Database and add a new row for it in the CalendarGrid immediately.
3. IF a user attempts to add a task with a name composed entirely of whitespace, THEN THE App SHALL reject the input and leave the CalendarGrid unchanged.
4. IF a user attempts to add a task with a name that already exists in the Database, THEN THE App SHALL reject the input and leave the CalendarGrid unchanged.
5. WHEN a task is successfully added, THE App SHALL clear the TaskNameField.

---

### Requirement 5: Persistent Storage

**User Story:** As a user, I want my tasks and completion records to survive application restarts, so that my history is never lost.

#### Acceptance Criteria

1. WHEN the App starts, THE Database SHALL be initialized with the `tasks` and `record` tables if they do not already exist.
2. WHEN a CompletionRecord is created or updated, THE Database SHALL immediately persist the change.
3. WHEN the App restarts, THE CalendarGrid SHALL reflect all Tasks and CompletionRecords previously saved to the Database.

---

### Requirement 6: Dark Theme

**User Story:** As a user, I want the app to have a dark theme, so that it is comfortable to use in low-light conditions.

#### Acceptance Criteria

1. THE App SHALL apply a dark background color (near-black or dark grey) to all UI surfaces.
2. THE App SHALL render all text in a light color (white or near-white) for sufficient contrast against the dark background.
3. THE App SHALL style interactive controls (checkboxes, buttons, text fields) consistently with the dark theme.
