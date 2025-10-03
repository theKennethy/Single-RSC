package org.nemotech.rsc.client;

import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import org.nemotech.rsc.Constants;
import org.nemotech.rsc.client.sound.MusicPlayer;
import org.nemotech.rsc.util.Util;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * JavaFX Shell - Base class for game client
 * Converted from AWT Panel to JavaFX Pane/Canvas
 */
public abstract class Shell extends Pane implements Runnable {
    
    /* abstract methods */
    
    protected abstract void startGame();
    protected abstract void handleInputs();
    protected abstract void onClosing();
    protected abstract void draw();
    protected abstract void handleKeyPress(int key);
    protected abstract void handleMouseScroll(int rotation);
    
    /* variables */
    
    private Font fontTimesRoman15, fontHelvetica13b, fontHelvetica12;
    private Image imageLogo;
    protected Canvas canvas;
    protected GraphicsContext gc;
    private Thread gameThread;
    
    protected MusicPlayer musicPlayer;
    protected Application application;
    
    private final String CHAR_MAP = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!\"\243$%^&*()-_=+[{]};:'@#~,<.>/?\\| ";
    
    public String logoHeaderText, inputTextCurrent, inputTextFinal, loadingProgessText;
    public boolean keyLeft, keyRight, keyUp, keyDown, keySpace, interlace, closing;
    public int threadSleep, mouseX, mouseY, mouseButtonDown, lastMouseButtonDown, stopTimeout, interlaceTimer,
        loadingProgressPercent, panelWidth, panelHeight, targetFps, maxDrawTime, loadingStep;
    private long[] timings;
    
    /* resizable client code */
    
    protected final java.awt.Dimension dimension = new java.awt.Dimension();
    protected boolean resized = true;
    
    protected void doResize(int width, int height) {
        synchronized (dimension) {
            dimension.width = width;
            dimension.height = height;
            resized = false;
            
            // Resize the canvas to match the window size
            if (canvas != null) {
                canvas.setWidth(width);
                canvas.setHeight(height);
            }
        }
    }
    
    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        // Make canvas fill the pane
        double width = getWidth();
        double height = getHeight();
        if (width > 0 && height > 0 && canvas != null) {
            canvas.setWidth(width);
            canvas.setHeight(height);
            // Trigger game resize
            doResize((int)width, (int)height);
        }
    }
    
    /* constructor */

    public Shell() {
        panelWidth = Constants.APPLICATION_WIDTH;
        panelHeight = Constants.APPLICATION_HEIGHT;
        targetFps = 20;
        maxDrawTime = 1000;
        timings = new long[10];
        loadingStep = 1;
        loadingProgessText = "Loading";
        fontTimesRoman15 = Font.font("Times New Roman", 15);
        fontHelvetica13b = Font.font("Helvetica", FontWeight.BOLD, 13);
        fontHelvetica12 = Font.font("Helvetica", 12);
        threadSleep = 10;
        inputTextCurrent = "";
        inputTextFinal = "";
        
        // Create canvas for rendering
        canvas = new Canvas(panelWidth, panelHeight);
        gc = canvas.getGraphicsContext2D();
        this.getChildren().add(canvas);
        
        // Make the Pane size to its preferred size
        this.setPrefSize(panelWidth, panelHeight);
        this.setMinSize(512, 344);
        
        // Setup input handlers
        setupInputHandlers();
    }

    private void setupInputHandlers() {
        // Keyboard events
        canvas.setFocusTraversable(true);
        canvas.setOnKeyPressed(this::handleKeyPressedEvent);
        canvas.setOnKeyReleased(this::handleKeyReleasedEvent);
        
        // Mouse events - also add CLICKED event
        canvas.setOnMousePressed(this::handleMousePressedEvent);
        canvas.setOnMouseReleased(this::handleMouseReleasedEvent);
        canvas.setOnMouseMoved(this::handleMouseMovedEvent);
        canvas.setOnMouseDragged(this::handleMouseDraggedEvent);
        canvas.setOnMouseClicked(e -> {
            // Make sure canvas has focus when clicked
            canvas.requestFocus();
        });
    }

    public void start() {
        System.out.println("[Classic Client] Loading process started...");
        panelWidth = Constants.APPLICATION_WIDTH;
        panelHeight = Constants.APPLICATION_HEIGHT;
        
        // Application will be set by MainFX
        doResize(panelWidth, panelHeight);
        canvas.requestFocus();
        
        loadingStep = 1;
        gameThread = new Thread(this);
        gameThread.start();
        gameThread.setPriority(1);
    }
    
    public void setApplication(Application app) {
        this.application = app;
    }

    protected void setTargetFps(int i) {
        targetFps = 1000 / i;
    }

    protected void resetTimings() {
        for(int i = 0; i < 10; i++) {
            timings[i] = 0L;
        }
    }
    
    public Application getApplication() {
        return application;
    }

    protected void handleKeyPressedEvent(KeyEvent e) {
        String text = e.getText();
        char chr = text.length() > 0 ? text.charAt(0) : 0;
        KeyCode code = e.getCode();
        
        if (chr != 0 && chr != 65535) {
            handleKeyPress(chr);
        }
        
        switch (code) {
            case LEFT:
                keyLeft = true;
                break;
            case RIGHT:
                keyRight = true;
                break;
            case UP:
                keyUp = true;
                break;
            case DOWN:
                keyDown = true;
                break;
            case SPACE:
                keySpace = true;
                break;
            case F1:
                interlace = !interlace;
                break;
            default:
                break;
        }
        
        boolean foundText = false;
        if (chr != 0) {
            for (int i = 0; i < CHAR_MAP.length(); i++) {
                if (CHAR_MAP.charAt(i) == chr) {
                    foundText = true;
                    break;
                }
            }
        }
        
        if (foundText) {
            if (inputTextCurrent.length() < 20) {
                inputTextCurrent += chr;
            }
        }
        
        if (code == KeyCode.BACK_SPACE) {
            if (inputTextCurrent.length() > 0) {
                inputTextCurrent = inputTextCurrent.substring(0, inputTextCurrent.length() - 1);
            }
        }
        
        if (code == KeyCode.ENTER) {
            inputTextFinal = inputTextCurrent;
        }
    }

    protected void handleKeyReleasedEvent(KeyEvent e) {
        KeyCode code = e.getCode();
        switch (code) {
            case LEFT:
                keyLeft = false;
                break;
            case RIGHT:
                keyRight = false;
                break;
            case UP:
                keyUp = false;
                break;
            case DOWN:
                keyDown = false;
                break;
            case SPACE:
                keySpace = false;
                break;
            default:
                break;
        }
    }

    protected void handleMouseMovedEvent(MouseEvent e) {
        mouseX = (int) e.getX();
        mouseY = (int) e.getY();
        mouseButtonDown = 0;
    }

    protected void handleMouseReleasedEvent(MouseEvent e) {
        mouseX = (int) e.getX();
        mouseY = (int) e.getY();
        mouseButtonDown = 0;
    }

    protected void handleMousePressedEvent(MouseEvent e) {
        int x = (int) e.getX();
        int y = (int) e.getY();
        mouseX = x;
        mouseY = y;
        if (e.getButton() == MouseButton.SECONDARY) {
            mouseButtonDown = 2;
        } else {
            mouseButtonDown = 1;
        }
        lastMouseButtonDown = mouseButtonDown;
        handleMouseDown(mouseButtonDown, x, y);
    }

    protected void handleMouseDown(int i, int j, int k) {
    }

    protected void handleMouseDraggedEvent(MouseEvent e) {
        mouseX = (int) e.getX();
        mouseY = (int) e.getY();
        if (e.getButton() == MouseButton.SECONDARY) {
            mouseButtonDown = 2;
        } else {
            mouseButtonDown = 1;
        }
    }
    
    protected void drawTextBox(String s, String s1) {
        Platform.runLater(() -> {
            Font font = Font.font("Sans Serif", FontWeight.BOLD, 15);
            int c = 512;
            int c1 = 344;
            gc.setFill(Color.BLACK);
            gc.fillRect(c / 2 - 140, c1 / 2 - 25, 280, 50);
            gc.setStroke(Color.WHITE);
            gc.strokeRect(c / 2 - 140, c1 / 2 - 25, 280, 50);
            drawString(s, font, c / 2, c1 / 2 - 10);
            drawString(s1, font, c / 2, c1 / 2 + 10);
        });
    }

    public void closeProgram() {
        closing = true;
        stopTimeout = -2;
        System.out.println("\nSaving player data and closing application...");
        if(musicPlayer != null && musicPlayer.isRunning()) {
            musicPlayer.stop();
        }
        if(musicPlayer != null) {
            musicPlayer.close();
        }
        if(application != null) {
            application.close();
        }
        onClosing();
        Platform.exit();
        System.exit(0);
    }

    @Override
    public void run() {
        if (loadingStep == 1) {
            loadingStep = 2;
            loadJagex();
            Platform.runLater(() -> drawLoadingScreen(0, "Loading..."));
            startGame();
            loadingStep = 0;
        }
        
        int i = 0;
        int j = 256;
        int sleep = 1;
        int i1 = 0;
        for (int j1 = 0; j1 < 10; j1++)
            timings[j1] = System.currentTimeMillis();

        while (stopTimeout >= 0) {
            if (stopTimeout > 0) {
                stopTimeout--;
                if (stopTimeout == 0) {
                    closeProgram();
                    gameThread = null;
                    return;
                }
            }
            
            int k1 = j;
            int lastSleep = sleep;
            j = 300;
            sleep = 1;
            long time = System.currentTimeMillis();
            
            if (timings[i] == 0L) {
                j = k1;
                sleep = lastSleep;
            } else if (time > timings[i])
                j = (int) ((long) (2560 * targetFps) / (time - timings[i]));
            
            if (j < 25)
                j = 25;
            if (j > 256) {
                j = 256;
                sleep = (int) ((long) targetFps - (time - timings[i]) / 10L);
                if (sleep < threadSleep)
                    sleep = threadSleep;
            }
            
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                /* ignore */
            }
            
            timings[i] = time;
            i = (i + 1) % 10;
            
            if (sleep > 1) {
                for (int j2 = 0; j2 < 10; j2++)
                    if (timings[j2] != 0L)
                        timings[j2] += sleep;
            }
            
            int k2 = 0;
            while (i1 < 256) {
                handleInputs();
                i1 += j;
                if (++k2 > maxDrawTime) {
                    i1 = 0;
                    interlaceTimer += 6;
                    if (interlaceTimer > 25) {
                        interlaceTimer = 0;
                        interlace = true;
                    }
                    break;
                }
            }
            
            interlaceTimer--;
            i1 &= 0xff;
            
            if(!closing) {
                draw();
            }
        }
        
        if(stopTimeout == -1) {
            closeProgram();
        }
        gameThread = null;
    }

    public void paint() {
        if(loadingStep == 2 && imageLogo != null) {
            drawLoadingScreen(loadingProgressPercent, loadingProgessText);
        }
    }

    private void loadJagex() {
        Platform.runLater(() -> {
            gc.setFill(Color.BLACK);
            gc.fillRect(0, 0, panelWidth, panelHeight);
        });
        
        byte buff[] = readDataFile("jagex.jag", "Jagex library", 0);
        if (buff != null) {
            byte logo[] = Util.unpackData("logo.tga", 0, buff);
            imageLogo = createImage(logo);
        }
        buff = readDataFile("fonts.jag", "Game fonts", 5);
        if (buff != null) {
            Surface.createFont(Util.unpackData("h11p.jf", 0, buff), 0);
            Surface.createFont(Util.unpackData("h12b.jf", 0, buff), 1);
            Surface.createFont(Util.unpackData("h12p.jf", 0, buff), 2);
            Surface.createFont(Util.unpackData("h13b.jf", 0, buff), 3);
            Surface.createFont(Util.unpackData("h14b.jf", 0, buff), 4);
            Surface.createFont(Util.unpackData("h16b.jf", 0, buff), 5);
            Surface.createFont(Util.unpackData("h20b.jf", 0, buff), 6);
            Surface.createFont(Util.unpackData("h24b.jf", 0, buff), 7);
        }
    }

    private void drawLoadingScreen(int percent, String text) {
        Platform.runLater(() -> {
            int midx = (panelWidth - 281) / 2;
            int midy = (panelHeight - 148) / 2;
            
            gc.setFill(Color.BLACK);
            gc.fillRect(0, 0, panelWidth, panelHeight + 10);
            
            if (imageLogo != null) {
                gc.drawImage(imageLogo, midx, midy);
            }
            
            midx += 2;
            midy += 90;
            loadingProgressPercent = percent;
            loadingProgessText = text;
            
            gc.setStroke(Color.rgb(132, 132, 132));
            gc.setFill(Color.rgb(132, 132, 132));
            gc.strokeRect(midx - 2, midy - 2, 280, 23);
            gc.fillRect(midx, midy, (277 * percent) / 100, 20);
            
            gc.setFill(Color.rgb(198, 198, 198));
            drawString(text, fontTimesRoman15, midx + 138, midy + 10);
            drawString("Created by JAGeX - visit www.jagex.com", fontHelvetica13b, midx + 138, midy + 30);
            drawString("\2512001-2003 Andrew Gower and Jagex Ltd", fontHelvetica13b, midx + 138, midy + 44);
            
            gc.setFill(Color.rgb(132, 132, 152));
            drawString("Serverless Modification by Zoso [sean@nemotech.org]", fontHelvetica12, midx + 138, panelHeight - 20);
            
            if (logoHeaderText != null) {
                gc.setFill(Color.WHITE);
                drawString(logoHeaderText, fontHelvetica13b, midx + 138, midy - 120);
            }
        });
    }

    protected void showLoadingProgress(int i, String s) {
        Platform.runLater(() -> {
            int j = (panelWidth - 281) / 2;
            int k = (panelHeight - 148) / 2;
            j += 2;
            k += 90;
            loadingProgressPercent = i;
            loadingProgessText = s;
            int l = (277 * i) / 100;
            
            gc.setFill(Color.rgb(132, 132, 132));
            gc.fillRect(j, k, l, 20);
            gc.setFill(Color.BLACK);
            gc.fillRect(j + l, k, 277 - l, 20);
            gc.setFill(Color.rgb(198, 198, 198));
            drawString(s, fontTimesRoman15, j + 138, k + 10);
        });
    }

    protected void drawString(String s, Font font, int i, int j) {
        gc.setFont(font);
        javafx.scene.text.Text text = new javafx.scene.text.Text(s);
        text.setFont(font);
        double width = text.getLayoutBounds().getWidth();
        double height = text.getLayoutBounds().getHeight();
        gc.fillText(s, i - width / 2, j + height / 4);
    }

    public Image createImage(byte buff[]) {
        int width = buff[13] * 256 + buff[12];
        int height = buff[15] * 256 + buff[14];
        int[] pixels = new int[width * height];
        
        // Extract palette
        byte[] r = new byte[256];
        byte[] g = new byte[256];
        byte[] b = new byte[256];
        for (int k = 0; k < 256; k++) {
            r[k] = buff[20 + k * 3];
            g[k] = buff[19 + k * 3];
            b[k] = buff[18 + k * 3];
        }

        // Convert indexed color to ARGB
        int l = 0;
        for (int i1 = height - 1; i1 >= 0; i1--) {
            for (int j1 = 0; j1 < width; j1++) {
                int index = buff[786 + j1 + i1 * width] & 0xFF;
                int red = r[index] & 0xFF;
                int green = g[index] & 0xFF;
                int blue = b[index] & 0xFF;
                pixels[l++] = (0xFF << 24) | (red << 16) | (green << 8) | blue;
            }
        }

        WritableImage image = new WritableImage(width, height);
        PixelWriter pw = image.getPixelWriter();
        pw.setPixels(0, 0, width, height, javafx.scene.image.PixelFormat.getIntArgbInstance(), pixels, 0, width);
        return image;
    }

    protected byte[] readDataFile(String file, String description, int percent) {
        file = Constants.CACHE_DIRECTORY + "jags/" + file;
        int archiveSize = 0;
        int archiveSizeCompressed = 0;
        byte archiveData[] = null;
        try {
            showLoadingProgress(percent, "Loading " + description + " - 0%");
            java.io.InputStream inputstream = Util.openFile(file);
            DataInputStream datainputstream = new DataInputStream(inputstream);
            byte header[] = new byte[6];
            datainputstream.readFully(header, 0, 6);
            archiveSize = ((header[0] & 0xff) << 16) + ((header[1] & 0xff) << 8) + (header[2] & 0xff);
            archiveSizeCompressed = ((header[3] & 0xff) << 16) + ((header[4] & 0xff) << 8) + (header[5] & 0xff);
            showLoadingProgress(percent, "Loading " + description + " - 5%");
            int read = 0;
            archiveData = new byte[archiveSizeCompressed];
            while (read < archiveSizeCompressed) {
                int length = archiveSizeCompressed - read;
                if (length > 1000)
                    length = 1000;
                datainputstream.readFully(archiveData, read, length);
                read += length;
                showLoadingProgress(percent, "Loading " + description + " - " + (5 + (read * 95) / archiveSizeCompressed) + "%");
            }
            datainputstream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        showLoadingProgress(percent, "Unpacking " + description);
        if (archiveSizeCompressed != archiveSize) {
            byte decompressed[] = new byte[archiveSize];
            BZLib.decompress(decompressed, archiveSize, archiveData, archiveSizeCompressed, 0);
            return decompressed;
        } else {
            return archiveData;
        }
    }

    public GraphicsContext getGraphicsContext() {
        return gc;
    }

    public Canvas getCanvas() {
        return canvas;
    }

    public Image createImage(int x, int y) {
        return new WritableImage(x, y);
    }
}
