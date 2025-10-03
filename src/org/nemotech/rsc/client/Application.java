package org.nemotech.rsc.client;

import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.nemotech.rsc.Constants;

/**
 * JavaFX Application window - converted from AWT Frame
 */
public class Application {

    private Shell shell;
    private Stage stage;
    private Scene scene;

    public Application(Shell shell, Stage stage) {
        this.shell = shell;
        this.stage = stage;
        
        stage.setTitle(Constants.APPLICATION_TITLE);
        stage.setResizable(Constants.APPLICATION_RESIZABLE);
        
        StackPane root = new StackPane(shell);
        scene = new Scene(root, Constants.APPLICATION_WIDTH, Constants.APPLICATION_HEIGHT);
        scene.setFill(javafx.scene.paint.Color.BLACK);
        
        // Make shell fill the StackPane
        shell.prefWidthProperty().bind(root.widthProperty());
        shell.prefHeightProperty().bind(root.heightProperty());
        
        stage.setMinWidth(512);
        stage.setMinHeight(344);
        
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        // Remove max size constraints to allow fullscreen
        // stage.setMaxWidth(Math.min(1535, screenBounds.getWidth()));
        // stage.setMaxHeight(Math.min(1800, screenBounds.getHeight()));
        
        // Enable maximizing to full screen
        stage.setMaximized(Constants.APPLICATION_START_MAXIMIZED);
        
        // Center the window (only if not starting maximized)
        if (!Constants.APPLICATION_START_MAXIMIZED) {
            stage.setX((screenBounds.getWidth() - Constants.APPLICATION_WIDTH) / 2);
            stage.setY((screenBounds.getHeight() - Constants.APPLICATION_HEIGHT) / 2);
        }
        
        addListeners();
        
        stage.setScene(scene);
        stage.show();
        
        // Request focus on canvas AFTER stage is shown
        Platform.runLater(() -> {
            shell.getCanvas().requestFocus();
        });
    }
    
    private void addListeners() {
        // Mouse wheel listener
        scene.addEventFilter(ScrollEvent.SCROLL, e -> {
            int rotation = e.getDeltaY() > 0 ? -1 : 1;
            shell.handleMouseScroll(rotation);
        });

        // Window close listener
        stage.setOnCloseRequest(e -> {
            shell.closeProgram();
        });

        // Window resize listener
        if (Constants.APPLICATION_RESIZABLE) {
            stage.widthProperty().addListener((obs, oldVal, newVal) -> {
                shell.doResize((int) stage.getWidth(), (int) stage.getHeight());
            });
            stage.heightProperty().addListener((obs, oldVal, newVal) -> {
                shell.doResize((int) stage.getWidth(), (int) stage.getHeight());
            });
        }
    }

    public Stage getStage() {
        return stage;
    }

    public Scene getScene() {
        return scene;
    }

    public void close() {
        Platform.runLater(() -> {
            if (stage != null) {
                stage.close();
            }
        });
    }
}
