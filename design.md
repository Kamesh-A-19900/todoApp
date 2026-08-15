# Design Document: Todo Calendar Grid

## Overview

The Todo Calendar Grid replaces the placeholder JavaFX UI with a full-featured calendar-grid view. Tasks are rows, days of the current month are columns, and a checkbox at each intersection tracks daily completion. Only today's column is interactive. The app persists all data to SQLite via the existing `Database` class and follows a dark ChatGPT-style theme.

---

## Architecture

The app follows the existing layered structure:

```
Main (JavaFX Application)
  └── CalendarController (FXML controller)
        ├── TodoService (business logic)
        │     ├── TodoDao (data access)
        │     │     └── Database (JDBC / SQLite)
        │     └── CompletionDao (data access)
        │           └── Database
        └── CalendarGridBuilder (UI helper — constructs the TableView)
```

- **Model layer**: `Todo`, `CompletionRecord`
- **DAO layer**: `TodoDao`, `CompletionDao`
- **Service layer**: `TodoService`
- **Controller layer**: `CalendarController`
- **UI**: `calendar.fxml` + `style.css`

The existing `TodoController`, `TodoDao`, `TodoService` stubs are replaced/filled by this design.

---

## Components and Interfaces

### Model: `Todo`

Already exists. Keep as-is (`todoId`, `todoName`, `todoDescription`).

### Model: `CompletionRecord`

```java
public class CompletionRecord {
    private int taskId;
    private LocalDate date;
    private boolean completed;
}
```

### DAO: `TodoDao`

```java
public interface TodoDaoInterface {
    List<Todo> getAllTodos();
    void addTodo(String name);              // throws if name blank or duplicate
    void deleteTodo(int todoId);
}
```

### DAO: `CompletionDao`

```java
public interface CompletionDaoInterface {
    // Returns all records for the current month
    Map<Integer, Set<LocalDate>> getCompletionsForMonth(int year, int month);
    void setCompletion(int taskId, LocalDate date, boolean completed);
}
```

### Service: `TodoService`

Thin orchestration layer:

```java
public class TodoService {
    public List<Todo> getTodos();
    public void addTodo(String name);     // validates non-blank, delegates to DAO
    public Map<Integer, Set<LocalDate>> getMonthCompletions(YearMonth month);
    public void toggleCompletion(int taskId, LocalDate date, boolean checked);
}
```

### Controller: `CalendarController`

Wired to `calendar.fxml`. Responsibilities:
- On init: call `TodoService` to load tasks and completions, then build the `TableView`.
- Add-task button handler: validate input, call service, append a new row.
- Checkbox toggle handler (only fires for today's column): call `service.toggleCompletion`.

### UI Helper: `CalendarGridBuilder`

Standalone class that programmatically constructs `TableView<Todo>` columns:
- Column 0: task name (`TableColumn<Todo, String>`).
- Columns 1–N: one per day of the current month (`TableColumn<Todo, Boolean>`).
  - Each cell factory creates a `CheckBoxTableCell`-style cell.
  - Cells for non-today columns have `setDisable(true)`.

---

## Data Models

### SQLite Schema (already in `Database.java`, no changes needed)

```sql
CREATE TABLE IF NOT EXISTS tasks (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT UNIQUE NOT NULL,
    description TEXT DEFAULT ''
);

CREATE TABLE IF NOT EXISTS record (
    id      INTEGER PRIMARY KEY AUTOINCREMENT,
    date    TEXT DEFAULT CURRENT_DATE,
    taskId  INTEGER NOT NULL,
    status  INTEGER NOT NULL DEFAULT 1 CHECK(status IN (0, 1)),
    FOREIGN KEY (taskId) REFERENCES tasks(id) ON DELETE CASCADE,
    UNIQUE(date, taskId)
);
```

- `status = 1` → completed, `status = 0` → not completed.
- An absent row is treated as unchecked (same as `status = 0`).
- `date` is stored as ISO-8601 string (`YYYY-MM-DD`).

### Data Flow: Loading the Grid

```
App start
  → TodoService.getTodos()           → SELECT * FROM tasks
  → TodoService.getMonthCompletions()→ SELECT taskId, date FROM record
                                         WHERE date LIKE '2025-07-%' AND status=1
  → CalendarGridBuilder.build(todos, completions, today)
  → TableView rendered
```

### Data Flow: Toggling a Checkbox

```
User clicks checkbox (today's column only)
  → CalendarController.onToggle(taskId, today, newValue)
  → TodoService.toggleCompletion(taskId, today, newValue)
  → CompletionDao.setCompletion(taskId, today, newValue)
  → INSERT OR REPLACE INTO record(date, taskId, status) VALUES(?, ?, ?)
```

---

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system — essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Completion round-trip persistence

*For any* task and any date, toggling a checkbox to a given state and then reloading the completions from the database should return that same state.

**Validates: Requirements 2.3, 5.2, 5.3**

---

### Property 2: Non-today columns are always disabled

*For any* calendar grid built for a given `today`, every cell whose column date is not equal to `today` should have its checkbox disabled.

**Validates: Requirements 3.1, 3.2**

---

### Property 3: Whitespace task names are rejected

*For any* string composed entirely of whitespace characters (spaces, tabs, newlines), calling `TodoService.addTodo()` with that string should throw or return an error and leave the task list unchanged.

**Validates: Requirements 4.3**

---

### Property 4: Duplicate task names are rejected

*For any* task name already present in the database, attempting to add it again should result in an error and the task count in the database should remain unchanged.

**Validates: Requirements 4.4**

---

### Property 5: Grid row count matches task count

*For any* set of tasks loaded from the database, the number of rows in the CalendarGrid (excluding the header) should equal the number of tasks returned by `TodoService.getTodos()`.

**Validates: Requirements 1.1, 4.2**

---

### Property 6: Grid column count matches days in month

*For any* month, the number of day columns in the CalendarGrid should equal the number of days in that month (28, 29, 30, or 31), plus one for the task-name column.

**Validates: Requirements 1.2**

---

## Error Handling

| Scenario | Behaviour |
|---|---|
| Blank / whitespace task name on add | Service throws `IllegalArgumentException`; controller shows inline error label |
| Duplicate task name | DAO throws (SQLite UNIQUE constraint); controller shows inline error label |
| Database unavailable on start | App shows an alert dialog and exits gracefully |
| Toggle on non-today cell | Controller ignores the event (cell is disabled; JavaFX prevents it) |

---

## Testing Strategy

### Unit Tests (JUnit 5)

Focus on specific examples and edge cases:

- `TodoService.addTodo("")` → throws
- `TodoService.addTodo("   ")` → throws
- `TodoService.addTodo("Buy milk")` called twice → second call throws
- `CalendarGridBuilder.build()` with 0 tasks → 0 data rows, correct column count
- `CompletionDao.setCompletion` then `getCompletionsForMonth` → round-trip check for a specific known date

### Property-Based Tests

Use **jqwik** (JUnit 5 compatible property-based testing library for Java) configured for a minimum of **100 tries** per property.

Each property test must reference its design property in a comment:
`// Feature: todo-calendar-grid, Property N: <property text>`

| Property | Test strategy |
|---|---|
| **Property 1** – Completion round-trip | Generate random `(taskId, date, boolean)` tuples; set then get; assert equality |
| **Property 2** – Non-today columns disabled | Generate random `YearMonth` + `today`; build grid; assert all non-today cells disabled |
| **Property 3** – Whitespace rejection | Generate arbitrary whitespace-only strings; assert `addTodo` rejects each |
| **Property 4** – Duplicate rejection | Generate random valid task name; add once; attempt add again; assert second fails |
| **Property 5** – Row count invariant | Generate list of N random tasks; build grid; assert row count = N |
| **Property 6** – Column count invariant | Generate random `YearMonth`; build grid; assert column count = `month.lengthOfMonth() + 1` |

**Configuration**: Each `@Property` annotated test runs with `@Property(tries = 100)`.
**Tag format**: `// Feature: todo-calendar-grid, Property N: <description>`
