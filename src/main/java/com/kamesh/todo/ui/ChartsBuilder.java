package com.kamesh.todo.ui;

import com.kamesh.todo.model.Todo;
import javafx.geometry.Insets;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

public class ChartsBuilder {

    /**
     * Builds a bar chart (completions per task) that fills its parent pane.
     */
    public static VBox buildBarChart(List<Todo> todos,
                                     Map<Integer, Integer> counts,
                                     YearMonth month) {

        Label title = new Label("Completions per Task  —  " + month.getMonth().toString()
                .charAt(0) + month.getMonth().toString().substring(1).toLowerCase()
                + " " + month.getYear());
        title.getStyleClass().add("chart-title");

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Task");
        xAxis.setTickLabelRotation(-35);

        NumberAxis yAxis = new NumberAxis(0, month.lengthOfMonth(), 5);
        yAxis.setLabel("Days completed");
        yAxis.setTickLabelFill(Color.web("#aaaaaa"));
        xAxis.setTickLabelFill(Color.web("#aaaaaa"));

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setBarGap(4);
        chart.setCategoryGap(18);
        chart.getStyleClass().add("dark-chart");
        VBox.setVgrow(chart, Priority.ALWAYS);
        chart.setMaxWidth(Double.MAX_VALUE);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        if (todos.isEmpty()) {
            series.getData().add(new XYChart.Data<>("No tasks", 0));
        } else {
            for (Todo t : todos) {
                int done = counts.getOrDefault(t.getTodoId(), 0);
                // Truncate label at 12 chars so it fits even with 15 tasks
                String label = t.getTodoName().length() > 12
                        ? t.getTodoName().substring(0, 11) + "…"
                        : t.getTodoName();
                XYChart.Data<String, Number> bar = new XYChart.Data<>(label, done);
                series.getData().add(bar);
            }
        }
        chart.getData().add(series);

        // Green bars — set style after nodes are created
        for (XYChart.Data<String, Number> d : series.getData()) {
            d.nodeProperty().addListener((obs, o, node) -> {
                if (node != null) node.setStyle("-fx-bar-fill: #10a37f;");
            });
        }

        VBox pane = new VBox(8, title, chart);
        pane.setPadding(new Insets(14, 14, 10, 14));
        pane.getStyleClass().add("chart-pane");
        VBox.setVgrow(chart, Priority.ALWAYS);
        return pane;
    }

    /**
     * Builds a pie chart (overall completion rate) that fills its parent pane.
     */
    public static VBox buildPieChart(List<Todo> todos,
                                     Map<Integer, Integer> counts,
                                     YearMonth month) {

        Label title = new Label("Overall Completion Rate");
        title.getStyleClass().add("chart-title");

        int totalPossible = todos.size() * month.lengthOfMonth();
        int totalDone     = counts.values().stream().mapToInt(Integer::intValue).sum();
        int totalMissed   = Math.max(0, totalPossible - totalDone);

        PieChart chart = new PieChart();
        chart.setAnimated(false);
        chart.setLegendVisible(true);
        chart.getStyleClass().add("dark-chart");
        VBox.setVgrow(chart, Priority.ALWAYS);
        chart.setMaxWidth(Double.MAX_VALUE);

        if (totalPossible == 0) {
            chart.getData().add(new PieChart.Data("No data yet", 1));
        } else {
            double pct   = 100.0 * totalDone / totalPossible;
            double mPct  = 100.0 - pct;
            chart.getData().add(new PieChart.Data(
                    String.format("Done  %.1f%%", pct),
                    totalDone == 0 ? 0.001 : totalDone));
            chart.getData().add(new PieChart.Data(
                    String.format("Missed  %.1f%%", mPct),
                    totalMissed == 0 ? 0.001 : totalMissed));
        }

        // Colour slices
        chart.getData().forEach(d -> d.nodeProperty().addListener((obs, o, node) -> {
            if (node == null) return;
            if      (d.getName().startsWith("Done"))    node.setStyle("-fx-pie-color: #10a37f;");
            else if (d.getName().startsWith("Missed"))  node.setStyle("-fx-pie-color: #3a3a3a;");
            else                                         node.setStyle("-fx-pie-color: #555555;");
        }));

        VBox pane = new VBox(8, title, chart);
        pane.setPadding(new Insets(14, 14, 10, 14));
        pane.getStyleClass().add("chart-pane");
        VBox.setVgrow(chart, Priority.ALWAYS);
        return pane;
    }
}
