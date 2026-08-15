# Todo — Personal Daily Task Calendar

A dark-themed JavaFX desktop app for tracking daily task completion across a monthly calendar grid.

## What it does

- Displays a grid where **rows = tasks**, **columns = days of the month**
- Each cell has a checkbox — only **today's column** is interactive
- Past and future dates are read-only
- Tasks are stored in a master list and **assigned to specific months**
- Once assigned to a month, a task cannot be removed
- Charts at the bottom show your completion stats for the viewed month
- Data persists in a local SQLite file (`todo.db`)

---

## Requirements

- Java 21+
- Maven 3.6+
- JavaFX 21 (bundled via Maven — no separate install needed)

---

## Quick start (development)

```bash
cd TodoApp
mvn javafx:run
```

---

## Install as a system app

Builds a fat JAR and installs a `todo` command + desktop entry:

```bash
cd TodoApp
bash install.sh
```

Then launch from anywhere:

```bash
todo
```

Or find **Todo** in your applications menu (GNOME / KDE / XFCE).

---

## Uninstall

```bash
bash uninstall.sh
```

---

## Build fat JAR manually

```bash
mvn package
# Output: target/todo-app.jar
```

Run it directly:

```bash
java -jar target/todo-app.jar
```

---

## Project structure

```
TodoApp/
├── src/main/java/com/kamesh/todo/
│   ├── Main.java                    # JavaFX entry point
│   ├── controller/
│   │   └── CalendarController.java  # UI controller
│   ├── dao/
│   │   ├── TodoDao.java             # Task data access
│   │   └── CompletionDao.java       # Completion record data access
│   ├── database/
│   │   └── Database.java            # SQLite connection + schema init
│   ├── model/
│   │   ├── Todo.java                # Task model
│   │   └── CompletionRecord.java    # Completion record model
│   ├── service/
│   │   └── TodoService.java         # Business logic layer
│   └── ui/
│       ├── CalendarGridBuilder.java # Builds the TableView grid
│       └── ChartsBuilder.java       # Builds bar + pie charts
├── src/main/resources/com/kamesh/todo/
│   ├── calendar.fxml                # Main layout
│   └── style.css                    # Dark theme styles
├── install.sh                       # Install as system command
├── uninstall.sh                     # Remove system install
├── todo.sh                          # Local launcher (no install)
└── pom.xml
```

---

## Data

- Database file: `todo.db` in the working directory (created on first run)
- After install, run `todo` from `~` so `todo.db` lives in your home directory

---

## How to use

| Action | How |
|---|---|
| Add a task to master list | ☰ menu → Add Task |
| Assign task to this month | Click **＋** in the right panel |
| Mark today complete | Click the checkbox in today's column |
| View another month | Use **‹** / **›** navigation (only months with data shown) |
| Edit task description | Click **✎** on any task — anytime |
| Edit task name | Click **✎** — only within 24 hrs of creation |
