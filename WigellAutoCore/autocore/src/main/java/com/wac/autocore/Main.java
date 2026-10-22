package com.wac.autocore;

import com.wac.autocore.ui.AutoCoreApp;

import javafx.application.Application;

/**
 * Entry point. JavaFX starts first and opens the application context in {@link AutoCoreApp}, so
 * the window gets the beans the configuration built instead of building its own storage.
 */
public class Main {

    /** The property JavaFX reads before it asks GTK how large a pixel is. */
    static final String SCALE_PROPERTY = "glass.gtk.uiScale";

    /** What an operator sets to ask for another scale, on a screen that needs one. */
    static final String SCALE_VARIABLE = "AUTOCORE_UI_SCALE";

    public static void main(String[] args) {
        chooseScale();
        Application.launch(AutoCoreApp.class, args);
    }

    /**
     * Draws the interface at scale 1 unless someone asked for another scale.
     *
     * JavaFX asks GTK how large everything should be, and the environment answers with GDK_SCALE.
     * A session that started before the monitor scale was lowered keeps the old value, and then
     * every button, every font and the whole window is drawn twice its size. This property
     * overrides the environment, so a stale value cannot reach the window. It is set here and not
     * only in start.sh, because a launch from the editor or from Maven never sees start.sh.
     */
    static void chooseScale() {
        if (System.getProperty(SCALE_PROPERTY) != null) {
            return;
        }
        String wanted = System.getenv(SCALE_VARIABLE);
        System.setProperty(SCALE_PROPERTY, wanted == null || wanted.isBlank() ? "1" : wanted.trim());
    }
}
