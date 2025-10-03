package org.nemotech.rsc;

import javafx.application.Application;
import javafx.stage.Stage;

import org.nemotech.rsc.client.mudclient;
import org.nemotech.rsc.client.action.ActionManager;
import org.nemotech.rsc.client.action.impl.SleepHandler;
import org.nemotech.rsc.client.update.UpdateManager;
import org.nemotech.rsc.core.EngineThread;
import org.nemotech.rsc.external.EntityManager;
import org.nemotech.rsc.plugins.PluginManager;

/**
 * JavaFX Main Entry Point for RSC Single Player
 * FULL JavaFX IMPLEMENTATION
 */
public class MainFX extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        System.out.println("Welcome to RSC Single Player v" + Constants.VERSION + " (Full JavaFX)\n");
        System.out.println("To report any bugs, exploits, missing or incorrect content, etc. you can contact");
        System.out.println("the developer at sean@nemotech.org [Sean Niemann / Zoso]\n");
        
        // Initialize game components
        EntityManager.init();
        
        // Initialize plugins
        try {
            PluginManager.getInstance().init();
        } catch(ReflectiveOperationException e) {
            e.printStackTrace();
            System.exit(1);
        }
        
        // Start core services
        new EngineThread().start();
        new ActionManager().init();
        new UpdateManager().init();
        ActionManager.get(SleepHandler.class).init();
        
        // Create the mudclient
        mudclient.INSTANCE = new mudclient();
        
        // Create the JavaFX Application wrapper (using fully qualified name to avoid conflict)
        org.nemotech.rsc.client.Application application = new org.nemotech.rsc.client.Application(mudclient.INSTANCE, primaryStage);
        mudclient.INSTANCE.setApplication(application);
        
        // Start the game (this starts the game thread)
        mudclient.INSTANCE.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
