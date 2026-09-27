# Todo — Personal Daily Task Calendar

A dark-themed JavaFX desktop app for tracking daily task completion across a monthly calendar grid.

## What it does

- Grid where **rows = tasks**, **columns = days of the month**
- Checkboxes — only **today's column** is interactive; past/future are read-only
- Tasks live in a master list; you assign them to specific months
- Once assigned to a month a task cannot be removed
- Bar chart + pie chart show your monthly completion stats
- Data persists in a local SQLite file (`todo.db`)

---

## Requirements (local)

- Java 21+
- Maven 3.6+

---

## Quick start (development)

```bash
git clone https://github.com/Kamesh-A-19900/todoApp.git
cd todoApp
mvn javafx:run
```

---

## Install as a Linux system command

```bash
bash install.sh
```

Then from anywhere:

```bash
todo
```

Uninstall:

```bash
bash uninstall.sh
```

---

## Run with Docker (any OS with X11)

### Linux

```bash
# Allow Docker to access your display
xhost +local:docker

# Build and run
docker compose up --build
```

Data persists in a Docker named volume (`todo-data`). Your `todo.db` survives container restarts.

### macOS

Install [XQuartz](https://www.xquartz.org/), then:

```bash
xhost +localhost
DISPLAY=host.docker.internal:0 docker compose up --build
```

### Windows

Install [VcXsrv](https://sourceforge.net/projects/vcxsrv/) or [X410](https://x410.app/), start it, then:

```bash
set DISPLAY=host.docker.internal:0.0
docker compose up --build
```

---

## Build fat JAR manually

```bash
mvn package
java --module-path target/libs \
     --add-modules javafx.controls,javafx.fxml \
     -cp target/todo-app.jar:target/libs/sqlite-jdbc-3.50.3.0.jar \
     com.kamesh.todo.Main
```

---

## Project structure

```
TodoApp/
├── src/main/java/com/kamesh/todo/
│   ├── Main.java
│   ├── controller/CalendarController.java
│   ├── dao/TodoDao.java
│   ├── dao/CompletionDao.java
│   ├── database/Database.java
│   ├── model/Todo.java
│   ├── model/CompletionRecord.java
│   ├── service/TodoService.java
│   ├── ui/CalendarGridBuilder.java
│   └── ui/ChartsBuilder.java
├── src/main/resources/com/kamesh/todo/
│   ├── calendar.fxml
│   └── style.css
├── Dockerfile
├── docker-compose.yml
├── install.sh
├── uninstall.sh
└── pom.xml
```

---

## How to use

| Action | How |
|---|---|
| Add a task to master list | ☰ → Add Task |
| Assign task to this month | Click **＋** in the right panel |
| Mark today complete | Click checkbox in today's column (green header) |
| View another month | **‹** / **›** nav (only months with data enabled) |
| Edit task description | Click **✎** — anytime |
| Edit task name | Click **✎** — only within 24 hrs of creation |
