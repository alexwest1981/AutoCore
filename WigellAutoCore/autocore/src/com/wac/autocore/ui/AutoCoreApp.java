package com.wac.autocore.ui;

import com.wac.autocore.data.Settings;
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

/** JavaFX-gränssnittet för Wigell AutoCore. */
public class AutoCoreApp extends Application {

    /** Key the language choice is stored under in the settings table. */
    public static final String LANGUAGE_KEY = "language";

    private final GarageSystem garage = new GarageSystem();

    @Override
    public void start(Stage primaryStage) {
        // Databasen är redan klar: Main kör Db.ensureReady() och garage-fältet ovan gör samma sak
        // i sin konstruktor. Ett initTables() här gav en andra "Databas redo" i loggen.
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

        // Initiera navigering och sidhanterare med SidebarView
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
        // Lås tema till Emerald
        ThemeManager.apply(scene, "emerald");

        primaryStage.setTitle("Wigell AutoCore");
        primaryStage.setScene(scene);
        primaryStage.show();

        router.navigate("overview");
    }

    /** Läser det senast valda språket från inställningarna. */
    public static void restoreLanguage() {
        I18n.setLanguage(Settings.get(LANGUAGE_KEY, I18n.DEFAULT_LANG));
    }

    /** Sparar språkbyten så att valet finns kvar vid nästa start. */
    public static void persistLanguageChanges() {
        I18n.addListener(lang -> Settings.put(LANGUAGE_KEY, lang));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
