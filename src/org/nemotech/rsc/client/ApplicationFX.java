package org.nemotech.rsc.client;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * JavaFX Entry Point for Single-RSC
 * Migrated from AWT/Swing to JavaFX.
 */
public class ApplicationFX extends Application {

    public static final int APPLICATION_WIDTH = 800; // Adjust as needed
    public static final int APPLICATION_HEIGHT = 600; // Adjust as needed

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Single-RSC - JavaFX");

        // Main drawing canvas
        Canvas canvas = new Canvas(APPLICATION_WIDTH, APPLICATION_HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // TODO: Hook up game loop and rendering logic here
        // e.g., AnimationTimer for game loop

        StackPane root = new StackPane(canvas);
        Scene scene = new Scene(root, APPLICATION_WIDTH, APPLICATION_HEIGHT);

        // TODO: Add event handling, game initialization, etc.

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}