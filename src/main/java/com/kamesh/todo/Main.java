package com.kamesh.todo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/kamesh/todo/calendar.fxml"));

        // Use full screen bounds
        javafx.geometry.Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        Scene scene = new Scene(loader.load(), bounds.getWidth(), bounds.getHeight());

        stage.setTitle("Todo");
        stage.setScene(scene);
        stage.setMaximized(true);   // fills the laptop screen
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
