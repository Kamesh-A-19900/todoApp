package com.kamesh.todo.controller;

import com.kamesh.todo.dao.CompletionDao;
import com.kamesh.todo.dao.TodoDao;
import com.kamesh.todo.database.Database;
import com.kamesh.todo.model.Todo;
import com.kamesh.todo.service.TodoService;
import com.kamesh.todo.ui.CalendarGridBuilder;
import com.kamesh.todo.ui.ChartsBuilder;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CalendarController {

    @FXML private VBox        drawerPane;
    @FXML private ScrollPane  gridScroll;
    @FXML private ScrollPane  taskPanelScroll;
    @FXML private VBox        taskPanel;
    @FXML private VBox        barPane;
    @FXML private VBox        piePane;
    @FXML private Button      prevMonthBtn;
    @FXML private Button      nextMonthBtn;
    @FXML private Label       monthLabel;

    private TodoService  service;
    private Connection   conn;
    private final LocalDate today = LocalDate.now();
    private YearMonth    viewedMonth = YearMonth.now();
    private boolean      drawerOpen  = false;

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MMMM yyyy");

    // ── Init ──────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        try {
            conn = Database.getConnection();
            Database.initializeDatabase(conn);
            service = new TodoService(new TodoDao(conn), new CompletionDao(conn));
            refresh();
        } catch (SQLException e) {
            showAlert("Database error: " + e.getMessage());
        }
    }

    // ── Drawer ────────────────────────────────────────────────────────────

    @FXML public void toggleMenu() {
        drawerOpen = !drawerOpen;
        drawerPane.setVisible(drawerOpen);
        drawerPane.setManaged(drawerOpen);
    }

    @FXML public void closeMenu() {
        drawerOpen = false;
        drawerPane.setVisible(false);
        drawerPane.setManaged(false);
    }

    // ── Month navigation ──────────────────────────────────────────────────

    @FXML public void prevMonth() { viewedMonth = viewedMonth.minusMonths(1); refresh(); }
    @FXML public void nextMonth() { viewedMonth = viewedMonth.plusMonths(1); refresh(); }

    private void updateNavBar(Set<YearMonth> dbMonths) {
        monthLabel.setText(viewedMonth.format(MONTH_FMT));
        prevMonthBtn.setDisable(!dbMonths.contains(viewedMonth.minusMonths(1)));
        nextMonthBtn.setDisable(!dbMonths.contains(viewedMonth.plusMonths(1))
                || viewedMonth.plusMonths(1).isAfter(YearMonth.now()));
    }

    // ── Add Task to master list ───────────────────────────────────────────

    @FXML
    public void openAddTask() {
        closeMenu();
        openTaskForm(null); // null = new task
    }

    /**
     * Opens the add/edit task popup.
     * Pass null for a new task, or a Todo to edit (only if within 24hr window).
     */
    private void openTaskForm(Todo existing) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle(existing == null ? "Add Task" : "Edit Task");
        popup.setResizable(false);

        Label nameLbl = new Label("Name:");
        nameLbl.getStyleClass().add("field-label");
        TextField nameField = new TextField(existing == null ? "" : existing.getTodoName());
        nameField.setPromptText("Task name (required)");
        nameField.getStyleClass().add("task-input");

        // Name is locked after 24hrs for existing tasks
        boolean nameLocked = existing != null && !existing.isEditable();
        if (nameLocked) {
            nameField.setEditable(false);
            nameField.setDisable(true);
            Label lockedHint = new Label("Name locked after 24 hrs");
            lockedHint.getStyleClass().add("locked-hint");
            nameLbl.setText("Name (locked):");
        }

        Label descLbl = new Label("Description:");
        descLbl.getStyleClass().add("field-label");
        TextArea descField = new TextArea(existing == null ? "" : existing.getTodoDescription());
        descField.setPromptText("Description (optional)");
        descField.setPrefRowCount(3);
        descField.setWrapText(true);
        descField.getStyleClass().add("task-input");

        Label errorLbl = new Label();
        errorLbl.getStyleClass().add("error-label");
        errorLbl.setWrapText(true);

        Button saveBtn   = new Button(existing == null ? "Add Task" : "Save");
        saveBtn.getStyleClass().add("add-btn");
        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("cancel-btn");

        HBox btnRow = new HBox(10, saveBtn, cancelBtn);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(10, nameLbl, nameField, descLbl, descField, errorLbl, btnRow);
        root.setPadding(new Insets(20));
        root.setPrefWidth(380);
        root.getStyleClass().add("popup-root");

        Scene scene = new Scene(root);
        scene.getStylesheets().add(
                getClass().getResource("/com/kamesh/todo/style.css").toExternalForm());
        popup.setScene(scene);

        saveBtn.setOnAction(e -> {
            String name = nameLocked
                    ? existing.getTodoName()   // keep original name if locked
                    : (nameField.getText() == null ? "" : nameField.getText().trim());
            if (name.isEmpty()) { errorLbl.setText("Task name cannot be blank."); return; }
            try {
                if (existing == null) {
                    service.addTodo(name, descField.getText());
                } else {
                    service.updateTodo(existing.getTodoId(), name, descField.getText(), existing);
                }
                popup.close();
                refresh();
            } catch (IllegalArgumentException | IllegalStateException ex) {
                errorLbl.setText(ex.getMessage());
            } catch (SQLException ex) {
                String msg = ex.getMessage();
                errorLbl.setText(msg != null && msg.contains("UNIQUE")
                        ? "A task with that name already exists."
                        : "Error: " + (msg != null ? msg : "unknown"));
            }
        });

        cancelBtn.setOnAction(e -> popup.close());
        if (!nameLocked) nameField.setOnAction(e -> saveBtn.fire());
        popup.show();
    }

    private void refreshTaskPanel() {
        try {
            List<Todo> unassigned = service.getUnassignedTasks(viewedMonth);
            VBox list = new VBox(4);
            list.setPadding(new Insets(8));

            if (unassigned.isEmpty()) {
                Label empty = new Label("All tasks assigned\nto this month.");
                empty.getStyleClass().add("task-panel-empty");
                empty.setWrapText(true);
                list.getChildren().add(empty);
            } else {
                for (Todo t : unassigned) {
                    HBox row = new HBox(6);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.getStyleClass().add("task-panel-row");

                    VBox info = new VBox(2);
                    HBox.setHgrow(info, Priority.ALWAYS);

                    Label nameLabel = new Label(t.getTodoName());
                    nameLabel.getStyleClass().add("task-panel-name");
                    info.getChildren().add(nameLabel);

                    if (t.getTodoDescription() != null && !t.getTodoDescription().isBlank()) {
                        Label descLabel = new Label(t.getTodoDescription());
                        descLabel.getStyleClass().add("task-panel-desc");
                        descLabel.setWrapText(true);
                        info.getChildren().add(descLabel);
                    }

                    row.getChildren().add(info);

                    // Edit button — always visible, opens form (name locked after 24hrs)
                    Button editBtn = new Button("✎");
                    editBtn.getStyleClass().add("edit-btn");
                    editBtn.setTooltip(new Tooltip("Edit task"));
                    editBtn.setOnAction(e -> openTaskForm(t));
                    row.getChildren().add(editBtn);

                    // Add-to-month button
                    Button addBtn = new Button("＋");
                    addBtn.getStyleClass().add("assign-btn");
                    addBtn.setTooltip(new Tooltip("Add to " + viewedMonth.format(MONTH_FMT)));
                    addBtn.setOnAction(e -> {
                        try {
                            service.assignToMonth(t.getTodoId(), viewedMonth);
                            refresh();
                        } catch (SQLException ex) {
                            showAlert("Error: " + ex.getMessage());
                        }
                    });
                    row.getChildren().add(addBtn);
                    list.getChildren().add(row);
                }
            }

            // Assigned tasks — no remove button, just edit
            List<Todo> assigned = service.getMonthlyTasks(viewedMonth);
            if (!assigned.isEmpty()) {
                Separator sep = new Separator();
                sep.getStyleClass().add("drawer-sep");
                Label hdr = new Label("In this month:");
                hdr.getStyleClass().add("task-panel-section");
                list.getChildren().addAll(sep, hdr);

                for (Todo t : assigned) {
                    HBox row = new HBox(6);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.getStyleClass().add("task-panel-row-assigned");

                    VBox info = new VBox(2);
                    HBox.setHgrow(info, Priority.ALWAYS);

                    Label nameLabel = new Label(t.getTodoName());
                    nameLabel.getStyleClass().add("task-panel-name");
                    info.getChildren().add(nameLabel);

                    if (t.getTodoDescription() != null && !t.getTodoDescription().isBlank()) {
                        Label descLabel = new Label(t.getTodoDescription());
                        descLabel.getStyleClass().add("task-panel-desc");
                        descLabel.setWrapText(true);
                        info.getChildren().add(descLabel);
                    }

                    row.getChildren().add(info);

                    // Edit button — always visible
                    Button editBtn = new Button("✎");
                    editBtn.getStyleClass().add("edit-btn");
                    editBtn.setTooltip(new Tooltip("Edit task"));
                    editBtn.setOnAction(e -> openTaskForm(t));
                    row.getChildren().add(editBtn);

                    list.getChildren().add(row);
                }
            }

            taskPanelScroll.setContent(list);

        } catch (SQLException e) {
            showAlert("Failed to load task panel: " + e.getMessage());
        }
    }

    // ── Full refresh ──────────────────────────────────────────────────────

    private void refresh() {
        try {
            List<YearMonth> monthList = service.getDistinctMonths();
            Set<YearMonth> dbMonths = new HashSet<>(monthList);
            dbMonths.add(YearMonth.now());
            updateNavBar(dbMonths);

            List<Todo> monthlyTasks = service.getMonthlyTasks(viewedMonth);
            Map<Integer, Set<LocalDate>> completions = service.getMonthCompletions(viewedMonth);
            Map<Integer, Integer> counts = service.getCompletionCounts(viewedMonth);

            LocalDate effectiveToday = viewedMonth.equals(YearMonth.now())
                    ? today : LocalDate.MIN;

            TableView<Todo> table = CalendarGridBuilder.build(
                    monthlyTasks, completions, viewedMonth, effectiveToday,
                    (taskId, checked) -> {
                        try {
                            service.toggleCompletion(taskId, today, checked);
                            Map<Integer, Integer> updated = service.getCompletionCounts(viewedMonth);
                            Platform.runLater(() -> refreshCharts(monthlyTasks, updated));
                        } catch (SQLException e) {
                            showAlert("Save error: " + e.getMessage());
                        }
                    });

            table.prefWidthProperty().bind(gridScroll.widthProperty().subtract(2));
            gridScroll.setContent(table);

            refreshCharts(monthlyTasks, counts);
            refreshTaskPanel();

            if (viewedMonth.equals(YearMonth.now())) {
                scrollToToday(table);
            } else {
                Platform.runLater(() -> gridScroll.setHvalue(0));
            }

        } catch (SQLException e) {
            showAlert("Failed to load: " + e.getMessage());
        }
    }

    private void refreshCharts(List<Todo> todos, Map<Integer, Integer> counts) {
        VBox bar = ChartsBuilder.buildBarChart(todos, counts, viewedMonth);
        VBox pie = ChartsBuilder.buildPieChart(todos, counts, viewedMonth);

        HBox.setHgrow(bar, Priority.ALWAYS);
        HBox.setHgrow(pie, Priority.ALWAYS);

        barPane.getChildren().setAll(bar.getChildren());
        barPane.setPadding(bar.getPadding());

        piePane.getChildren().setAll(pie.getChildren());
        piePane.setPadding(pie.getPadding());
    }

    private void scrollToToday(TableView<Todo> table) {
        int totalCols = table.getColumns().size();
        if (totalCols <= 1) return;
        double hPos = (double) today.getDayOfMonth() / (totalCols - 1);
        Platform.runLater(() -> gridScroll.setHvalue(hPos));
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.showAndWait();
    }
}
