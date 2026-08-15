package com.kamesh.todo.ui;

import com.kamesh.todo.model.Todo;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

public class CalendarGridBuilder {

    private static final double DAY_COL_WIDTH = 38;
    private static final double NAME_COL_MIN  = 160;

    public static TableView<Todo> build(
            List<Todo> todos,
            Map<Integer, Set<LocalDate>> completions,
            YearMonth month,
            LocalDate today,
            BiConsumer<Integer, Boolean> onToggle) {

        TableView<Todo> table = new TableView<>();
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.getStyleClass().add("calendar-table");
        table.setPlaceholder(new Label("No tasks in calendar — use ☰ > Choose Calendar Tasks"));

        // ── Column 0: Task name ──────────────────────────────────────────
        TableColumn<Todo, String> nameCol = new TableColumn<>("Task");
        nameCol.setPrefWidth(NAME_COL_MIN);
        nameCol.setMinWidth(NAME_COL_MIN);
        nameCol.setResizable(true);
        nameCol.setSortable(false);
        nameCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getTodoName()));
        nameCol.setCellFactory(col -> {
            TableCell<Todo, String> cell = new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setText(null); setTooltip(null); return; }
                    setText(item);
                    // Show description in tooltip if available
                    Todo t = getTableView().getItems().get(getIndex());
                    if (t != null && t.getTodoDescription() != null && !t.getTodoDescription().isBlank()) {
                        setTooltip(new Tooltip(t.getTodoDescription()));
                    }
                }
            };
            cell.setAlignment(Pos.CENTER_LEFT);
            return cell;
        });
        table.getColumns().add(nameCol);

        // ── Columns 1..N: days ───────────────────────────────────────────
        int daysInMonth = month.lengthOfMonth();
        for (int day = 1; day <= daysInMonth; day++) {
            final LocalDate colDate = month.atDay(day);
            final boolean isToday = colDate.equals(today);

            TableColumn<Todo, Void> dayCol = new TableColumn<>(String.valueOf(day));
            dayCol.setPrefWidth(DAY_COL_WIDTH);
            dayCol.setMinWidth(DAY_COL_WIDTH);
            dayCol.setMaxWidth(DAY_COL_WIDTH);
            dayCol.setResizable(false);
            dayCol.setSortable(false);

            if (isToday) {
                dayCol.getStyleClass().add("today-column");
            }

            dayCol.setCellFactory(col -> new TableCell<>() {
                private final CheckBox cb = new CheckBox();

                {
                    cb.setDisable(!isToday);
                    cb.setOnAction(e -> {
                        int idx = getIndex();
                        if (idx < 0 || idx >= getTableView().getItems().size()) return;
                        Todo todo = getTableView().getItems().get(idx);
                        onToggle.accept(todo.getTodoId(), cb.isSelected());
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                        setGraphic(null);
                        return;
                    }
                    Todo todo = getTableView().getItems().get(getIndex());
                    Set<LocalDate> done = completions.get(todo.getTodoId());
                    cb.setSelected(done != null && done.contains(colDate));
                    HBox box = new HBox(cb);
                    box.setAlignment(Pos.CENTER);
                    setGraphic(box);
                }
            });

            table.getColumns().add(dayCol);
        }

        table.getItems().setAll(todos);
        return table;
    }
}
