package org.nemotech.rsc.client;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;

/**
 * JavaFX version of Shell. Handles core game loop, rendering, and input.
 */
public abstract class ShellFX extends Pane {

    protected Canvas canvas;
    protected GraphicsContext gc;
    protected AnimationTimer gameLoop;

    // Example input state variables
    protected boolean keyLeft, keyRight, keyUp, keyDown, keySpace;
    protected double mouseX, mouseY;
    protected boolean mouseButtonDown;

    public ShellFX(int width, int height) {
        canvas = new Canvas(width, height);
        gc = canvas.getGraphicsContext2D();
        this.getChildren().add(canvas);

        // Set up event handlers
        setupEvents();

        // Set up game loop
        setupGameLoop();
    }

    private void setupEvents() {
        // Mouse events
        canvas.setOnMousePressed((MouseEvent e) -> {
            mouseButtonDown = true;
            mouseX = e.getX();
            mouseY = e.getY();
            handleMousePressed(e);
        });

        canvas.setOnMouseReleased((MouseEvent e) -> {
            mouseButtonDown = false;
            handleMouseReleased(e);
        });

        canvas.setOnMouseMoved((MouseEvent e) -> {
            mouseX = e.getX();
            mouseY = e.getY();
            handleMouseMoved(e);
        });

        // Key events
        canvas.setFocusTraversable(true); // Allow canvas to receive key events
        canvas.setOnKeyPressed((KeyEvent e) -> {
            switch (e.getCode()) {
                case LEFT: keyLeft = true; break;
                case RIGHT: keyRight = true; break;
                case UP: keyUp = true; break;
                case DOWN: keyDown = true; break;
                case SPACE: keySpace = true; break;
                default: handleKeyPress(e);
            }
        });
        canvas.setOnKeyReleased((KeyEvent e) -> {
            switch (e.getCode()) {
                case LEFT: keyLeft = false; break;
                case RIGHT: keyRight = false; break;
                case UP: keyUp = false; break;
                case DOWN: keyDown = false; break;
                case SPACE: keySpace = false; break;
                default: handleKeyRelease(e);
            }
        });
    }

    private void setupGameLoop() {
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                update(); // game logic
                render(); // drawing
            }
        };
        gameLoop.start();
    }

    // Override these for game-specific logic
    protected abstract void update();
    protected abstract void render();

    // Optional overrides for input
    protected void handleKeyPress(KeyEvent e) {}
    protected void handleKeyRelease(KeyEvent e) {}
    protected void handleMousePressed(MouseEvent e) {}
    protected void handleMouseReleased(MouseEvent e) {}
    protected void handleMouseMoved(MouseEvent e) {}

    // You can expand with additional events, resize handling, etc.
}