package com.wac.autocore;

import com.wac.autocore.ui.AutoCoreApp;

import javafx.application.Application;

/**
 * Entry point. JavaFX starts first and opens the application context in {@link AutoCoreApp}, so
 * the window gets the beans the configuration built instead of building its own storage.
 */
public class Main {

    public static void main(String[] args) {
        Application.launch(AutoCoreApp.class, args);
    }
}
