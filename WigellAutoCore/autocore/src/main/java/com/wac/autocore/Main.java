package com.wac.autocore;

import com.wac.autocore.data.Db;
import com.wac.autocore.ui.AutoCoreApp;

import javafx.application.Application;
import org.springframework.boot.SpringApplication;

/**
 * Entry point: prepares the database and opens the interface.
 * The console version is still runnable in {@link ConsoleApp}.
 */
public class Main {

    public static void main(String[] args) {
        SpringApplication.run(AutoCoreApplication.class, args);
        Db.use(Db.pathFrom(args));
        try {
            Db.ensureReady();
        } catch (RuntimeException e) {
            // Without a finished database the window opens empty and still looks like
            // it works. Say what is wrong and open nothing, instead of half an app.
            System.out.println(e.getMessage());
            System.out.println("Databasen är inte körbar. Åtgärda felet och starta om.");
            System.exit(1);
        }
        Application.launch(AutoCoreApp.class, args);
    }
}
