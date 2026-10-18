package com.wac.autocore.ui;

import com.wac.autocore.exception.AppException;
import com.wac.autocore.data.Db;
import com.wac.autocore.repository.SqliteRepositoryFactory;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.theme.ThemeManager;
import com.wac.autocore.ui.i18n.I18n;
import com.wac.autocore.ui.navigation.PageRouter;
import com.wac.autocore.ui.navigation.SidebarView;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/** The JavaFX interface for Wigell AutoCore. */
public class AutoCoreApp extends Application {

    /** Key the language choice is stored under in the settings table. */
    public static final String LANGUAGE_KEY = "language";

    private final GarageSystem garage = new GarageSystem(new SqliteRepositoryFactory());

    @Override
    public void start(Stage primaryStage) {
        // The database is already ready: Main runs Db.ensureReady() and the garage field above
        // does the same in its constructor. An initTables() here gave a second "Databas redo".
        installErrorBoundary();
        restoreLanguage();
        persistLanguageChanges();

        BorderPane stage = new BorderPane();
        stage.setPadding(Insets.EMPTY);
        stage.getStyleClass().addAll("root", "stage");

        BorderPane shell = new BorderPane();
        shell.getStyleClass().add("shell");

        VBox pageBox = new VBox(18);
        pageBox.getStyleClass().add("pages");

        ScrollPane scroll = new ScrollPane(pageBox);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        // Set up navigation and the page handler with SidebarView
        PageRouter router = new PageRouter(garage, pageBox);
        SidebarView sidebar = new SidebarView(router::navigate);
        router.setSidebar(sidebar);

        BorderPane mainCol = new BorderPane();
        mainCol.getStyleClass().add("col");

        mainCol.setCenter(scroll);

        shell.setLeft(sidebar.getView());
        shell.setCenter(mainCol);
        stage.setCenter(shell);

        Scene scene = new Scene(stage, 1280, 800);
        ThemeManager.apply(scene, com.wac.autocore.theme.ThemeCatalog.DEFAULT_SLUG);
        com.wac.autocore.ui.motion.PressDepth.install(scene);

        primaryStage.setTitle("Wigell AutoCore");
        primaryStage.setScene(scene);
        primaryStage.show();

        router.navigate("overview");
    }

    /**
     * Shows our own failures as a sentence instead of a stack trace. One place covers every screen.
     */
    public static void installErrorBoundary() {
        Thread.setDefaultUncaughtExceptionHandler((thread, failure) -> {
            // A refusal with a sentence is the program working; only real faults need the trace.
            if (!(failure instanceof AppException) || ((AppException) failure).getMessageKey() == null) {
                failure.printStackTrace();
            }
            ActionDialogs.showError(I18n.get("dialog.confirm.title"), errorMessageFor(failure));
        });
    }

    /** The one place that turns an error into a text, so the same error reads the same everywhere. */
    public static String errorMessageFor(Throwable failure) {
        if (failure instanceof AppException) {
            String key = ((AppException) failure).getMessageKey();
            if (key != null) {
                // A refusal the service has already worded: the sentence alone is the answer.
                return I18n.get(key);
            }
            return I18n.get("view.error.action_failed") + "\n\n" + failure.getMessage();
        }
        return I18n.get("view.error.action_failed") + "\n\n" + failure.toString();
    }

    /** Reads the last chosen language from the settings. */
    public void restoreLanguage() {
        I18n.setLanguage(garage.getSetting(LANGUAGE_KEY, I18n.DEFAULT_LANG));
    }

    /** Saves language switches so the choice survives a restart. */
    public void persistLanguageChanges() {
        I18n.addListener(lang -> garage.saveSetting(LANGUAGE_KEY, lang));
    }

    public static void main(String[] args) {
        Db.use(Db.pathFrom(args));
        launch(args);
    }
}
