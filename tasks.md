# Implementation Plan: Todo Calendar Grid

## Overview

Build the calendar-grid todo tracker on top of the existing JavaFX + Maven + SQLite skeleton. Work layer by layer — models → DAOs → service → UI — so each step is immediately testable before the next begins.

## Tasks

- [x] 1. Add models and extend database schema
  - [x] 1.1 Create `CompletionRecord` model class
    - Add `CompletionRecord.java` in `com.kamesh.todo.model` with fields `taskId`, `date` (`LocalDate`), `completed`
    - _Requirements: 2.2, 2.3_
  - [x] 1.2 Verify `Database.initializeDatabase` creates both tables
    - Confirm `tasks` and `record` DDL in `Database.java` match the design schema; fix any discrepancies
    - _Requirements: 5.1_

- [x] 2. Implement `TodoDao`
  - [x] 2.1 Implement `getAllTodos()` and `addTodo(String name)` in `TodoDao`
    - `getAllTodos()`: `SELECT id, name FROM tasks ORDER BY id`
    - `addTodo(name)`: `INSERT INTO tasks(name) VALUES(?)` — let SQLite UNIQUE constraint propagate as `SQLException`
    - _Requirements: 1.1, 4.2, 4.4_
  - [ ]* 2.2 Write unit tests for `TodoDao`
    - Use an in-memory SQLite DB (`jdbc:sqlite::memory:`) initialised with `Database.initializeDatabase`
    - Test: `addTodo` then `getAllTodos` returns the task
    - Test: `addTodo("")` / `addTodo("  ")` — service-level; DAO receives only pre-validated names
    - Test: duplicate name throws `SQLException`
    - _Requirements: 4.2, 4.4_

- [x] 3. Implement `CompletionDao`
  - [x] 3.1 Create `CompletionDao.java` in `com.kamesh.todo.dao`
    - `getCompletionsForMonth(int year, int month)`: query `record` table for rows in that month with `status=1`; return `Map<Integer, Set<LocalDate>>` (taskId → set of completed dates)
    - `setCompletion(int taskId, LocalDate date, boolean completed)`: `INSERT OR REPLACE INTO record(date, taskId, status) VALUES(?,?,?)`
    - _Requirements: 2.2, 2.3, 5.2_
  - [ ]* 3.2 Write property test for completion round-trip (Property 1)
    - `// Feature: todo-calendar-grid, Property 1: completion round-trip persistence`
    - Use jqwik; generate random `(taskId, LocalDate within current month, boolean)` — call `setCompletion`, then `getCompletionsForMonth`, assert state matches
    - Run with `@Property(tries = 100)` on in-memory SQLite DB
    - **Property 1: Completion round-trip persistence** — Validates: Requirements 2.3, 5.2, 5.3

- [x] 4. Implement `TodoService`
  - [x] 4.1 Fill in `TodoService.java`
    - `getTodos()`: delegate to `TodoDao.getAllTodos()`
    - `addTodo(String name)`: trim → throw `IllegalArgumentException` if blank → delegate to `TodoDao.addTodo()`
    - `getMonthCompletions(YearMonth)`: delegate to `CompletionDao.getCompletionsForMonth()`
    - `toggleCompletion(int taskId, LocalDate date, boolean checked)`: delegate to `CompletionDao.setCompletion()`
    - _Requirements: 4.2, 4.3, 4.4_
  - [ ]* 4.2 Write property test for whitespace rejection (Property 3)
    - `// Feature: todo-calendar-grid, Property 3: whitespace task names are rejected`
    - Generate arbitrary whitespace-only strings; assert `addTodo` throws `IllegalArgumentException` and task list is unchanged
    - **Property 3: Whitespace task names are rejected** — Validates: Requirements 4.3
  - [ ]* 4.3 Write property test for duplicate rejection (Property 4)
    - `// Feature: todo-calendar-grid, Property 4: duplicate task names are rejected`
    - Generate random valid name; add once; add again; assert second call throws and task count is unchanged
    - **Property 4: Duplicate task names are rejected** — Validates: Requirements 4.4

- [x] 5. Checkpoint — Ensure all tests pass
  - Run `mvn test`; fix any failures before continuing. Ask if anything is unclear.

- [x] 6. Build `CalendarGridBuilder`
  - [x] 6.1 Create `CalendarGridBuilder.java` in `com.kamesh.todo.ui` (new package)
    - Static `build(List<Todo> todos, Map<Integer, Set<LocalDate>> completions, YearMonth month, LocalDate today, BiConsumer<Integer, Boolean> onToggle)` returns a configured `TableView<Todo>`
    - Column 0: task-name `TableColumn<Todo, String>` (non-editable)
    - Columns 1…N: one `TableColumn` per day; cell factory returns a `CheckBox` node; disabled if column date ≠ today; pre-checked if taskId/date in completions map; `setOnAction` calls `onToggle`
    - _Requirements: 1.1, 1.2, 1.3, 2.1, 2.2, 3.1, 3.2_
  - [ ]* 6.2 Write property test for non-today columns disabled (Property 2)
    - `// Feature: todo-calendar-grid, Property 2: non-today columns are always disabled`
    - Generate random `YearMonth` and `today` within that month; build grid; iterate all day columns; assert non-today cell nodes are disabled, today's are enabled
    - **Property 2: Non-today columns are always disabled** — Validates: Requirements 3.1, 3.2
  - [ ]* 6.3 Write property test for row count invariant (Property 5)
    - `// Feature: todo-calendar-grid, Property 5: grid row count matches task count`
    - Generate list of N random `Todo` objects; build grid; assert `tableView.getItems().size() == N`
    - **Property 5: Grid row count matches task count** — Validates: Requirements 1.1, 4.2
  - [ ]* 6.4 Write property test for column count invariant (Property 6)
    - `// Feature: todo-calendar-grid, Property 6: grid column count matches days in month`
    - Generate random `YearMonth`; build grid; assert `tableView.getColumns().size() == month.lengthOfMonth() + 1`
    - **Property 6: Grid column count matches days in month** — Validates: Requirements 1.2

- [x] 7. Build the FXML layout and controller
  - [x] 7.1 Create `calendar.fxml` in `src/main/resources/com/kamesh/todo/`
    - Root: `BorderPane` with `fx:controller="com.kamesh.todo.controller.CalendarController"`
    - Top: `HBox` containing `TextField` (fx:id="taskNameField"), `Button` (fx:id="addButton"), `Label` (fx:id="errorLabel")
    - Center: `ScrollPane` (fx:id="gridScroll") — `CalendarGridBuilder` injects the `TableView` programmatically
    - _Requirements: 1.4, 4.1_
  - [x] 7.2 Implement `CalendarController.java`
    - `@FXML` inject `taskNameField`, `addButton`, `errorLabel`, `gridScroll`
    - `initialize()`: open DB, call service to load data, call `CalendarGridBuilder.build()`, set result as `gridScroll.setContent()`; scroll to today's column
    - `onAddTask()`: trim input → call `service.addTodo()` → on success: reload grid, clear field, clear error; on `IllegalArgumentException` or duplicate exception: set `errorLabel` text
    - _Requirements: 1.1, 1.2, 1.4, 4.1, 4.2, 4.3, 4.4, 4.5_

- [x] 8. Wire `Main.java` to load `calendar.fxml`
  - Replace the placeholder `StackPane` in `Main.java` with an `FXMLLoader` that loads `calendar.fxml`
  - Set initial scene size to 1000×600; set stage title to "Todo"
  - Initialize the database on startup via `Database.initializeDatabase(conn)`
  - _Requirements: 1.1, 5.1_

- [x] 9. Apply dark theme CSS
  - Fill in `style.css` with dark-theme rules:
    - `.root`: `#1e1e1e` background
    - `Label`, `TableCell`: `#e0e0e0` text fill
    - `TableView`, `TableColumn`: dark background, matching border colors
    - `TextField`, `Button`: dark input styling with light placeholder/text
    - `CheckBox`: styled to be visible on dark background
  - Reference `style.css` from `calendar.fxml` via `<stylesheets>`
  - _Requirements: 6.1, 6.2, 6.3_

- [ ] 10. Add jqwik dependency to `pom.xml`
  - Add jqwik `1.8.5` and JUnit 5 (`junit-jupiter 5.10.2`) to `<dependencies>` with `<scope>test</scope>`
  - Add `maven-surefire-plugin 3.2.5` to `<build><plugins>` so JUnit 5 tests are discovered
  - _Requirements: testing infrastructure_

- [x] 11. Final checkpoint — Ensure all tests pass
  - Run `mvn test`; confirm all unit and property tests are green. Ask the user if anything needs adjustment.

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP
- Each task references specific requirements for traceability
- Property tests use jqwik with `@Property(tries = 100)` minimum
- Unit tests use an in-memory SQLite DB to avoid touching the real `todo.db`
- `CalendarGridBuilder` is pure logic (no `Application.launch`) so it can be tested outside the JavaFX thread with a `Platform.startup()` call in the test setup
