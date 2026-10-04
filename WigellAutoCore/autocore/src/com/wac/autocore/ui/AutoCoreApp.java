package com.wac.autocore.ui;

import com.wac.autocore.config.FeatureFlags;
import com.wac.autocore.data.Settings;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.theme.ThemeManager;
import com.wac.autocore.ui.components.SearchDropdown;
import com.wac.autocore.ui.i18n.I18n;
import com.wac.autocore.ui.navigation.PageRouter;
import com.wac.autocore.ui.navigation.SidebarView;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * JavaFX GUI for Wigell AutoCore.
 *
 * Sprint 2:
 * - Låst till tema Emerald (temaväljare borttagen).
 * - Enligt beställaren körs uteslutande sidebar-navigering (väljaren för top bar borttagen).
 * - Sökfältet ligger överst i huvudfönstret tillsammans med den granulära dropdownen, och slås
 *   av och på med växeln globalSearch i config/features.properties.
 */
public class AutoCoreApp extends Application {

    /** Key the language choice is stored under in the settings table. */
    public static final String LANGUAGE_KEY = "language";

    private final GarageSystem garage = new GarageSystem();

    @Override
    public void start(Stage primaryStage) {
        com.wac.autocore.data.Db.initTables();

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

        // Sökfältet ligger överst i huvudfönstret: samma rad oavsett vilken sida du står på.
        // Är växeln globalSearch av byggs ingen rad, och menyn ser ut som beställaren ville.
        VBox searchBar = buildSearchBar(garage, router);
        if (searchBar != null) {
            mainCol.setTop(searchBar);
        }

        // Sidebar används permanent enligt beställarens önskemål
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

    /**
     * Restores the language chosen on a previous run. Public and JavaFX-free so the test suite can
     * exercise the restore path without starting a toolkit.
     */
    public static void restoreLanguage() {
        I18n.setLanguage(Settings.get(LANGUAGE_KEY, I18n.DEFAULT_LANG));
    }

    /**
     * Saves the language to the settings table every time it changes, so the choice made with the
     * sidebar switch is still there after a restart. A listener rather than a call in the switch
     * itself, so every writer is covered and not only the one button.
     */
    public static void persistLanguageChanges() {
        I18n.addListener(lang -> Settings.put(LANGUAGE_KEY, lang));
    }

    /**
     * Sökfältet överst i huvudfönstret, med den granulära dropdownen under sig: skriver du ett
     * tecken söks hela systemet igenom och träffarna visas kategoriserade direkt under fältet,
     * oavsett vilken sida du står på. Enter öppnar den fullständiga sökvyn.
     *
     * Statisk och utan Stage, så sviten kan bygga raden utan att starta ett fönster.
     * Null när funktionen är avstängd i config/features.properties.
     */
    public static VBox buildSearchBar(GarageSystem garage, PageRouter router) {
        if (!FeatureFlags.isEnabled("globalSearch")) {
            return null;
        }

        TextField searchField = new TextField();
        searchField.getStyleClass().add("search");
        searchField.setPromptText(I18n.get("search.placeholder"));
        searchField.setPrefWidth(320);
        searchField.setOnAction(e -> router.smartNavigateForSearch(searchField.getText()));
        I18n.addListener(lang -> searchField.setPromptText(I18n.get("search.placeholder")));

        SearchDropdown.attach(searchField, garage, router);

        HBox row = new HBox(14, searchField);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 22, 10, 22));

        VBox bar = new VBox(0, row);
        bar.getStyleClass().add("topbar");
        return bar;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
