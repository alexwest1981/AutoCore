import com.wac.autocore.data.Db;
import com.wac.autocore.ui.AutoCoreApp;

import javafx.application.Application;

/**
 * Entry point: prepares the database and opens the interface.
 * The console version is still runnable in {@link ConsoleApp}.
 */
public class Main {

    public static void main(String[] args) {
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
