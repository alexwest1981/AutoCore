import com.wac.autocore.data.Db;
import com.wac.autocore.ui.AutoCoreApp;

import javafx.application.Application;

/**
 * Entry point for Wigell AutoCore.
 *
 * Starting Main opens the JavaFX GUI, which shows everything that exists in
 * the system today (read-only) using the generated theme system. The old
 * console application is kept runnable in {@link ConsoleApp}.
 */
public class Main {

    public static void main(String[] args) {
        Db.ensureReady();
        Application.launch(AutoCoreApp.class, args);
    }
}
