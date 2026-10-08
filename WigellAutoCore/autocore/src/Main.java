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
        try {
            Db.ensureReady();
        } catch (RuntimeException e) {
            // Utan en färdig databas blir fönstret tomt och ser ändå ut att fungera.
            // Säg vad som är fel och öppna ingenting, i stället för en halv app.
            System.out.println(e.getMessage());
            System.out.println("Databasen är inte körbar. Åtgärda felet och starta om.");
            System.exit(1);
        }
        Application.launch(AutoCoreApp.class, args);
    }
}
