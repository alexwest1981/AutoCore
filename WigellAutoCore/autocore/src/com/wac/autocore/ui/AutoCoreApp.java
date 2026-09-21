package com.wac.autocore.ui;

import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.theme.ThemeManager;
import com.wac.autocore.ui.navigation.PageRouter;
import com.wac.autocore.ui.navigation.SidebarView;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * JavaFX GUI for Wigell AutoCore.
 *
 * Sprint 2:
 * - Låst till tema Emerald (temaväljare borttagen).
 * - Enligt beställaren körs uteslutande sidebar-navigering (väljaren för top bar borttagen).
 * - Sökfältet är tills vidare bortkommenterat.
 */
public class AutoCoreApp extends Application {

    private final GarageSystem garage = new GarageSystem();

    @Override
    public void start(Stage primaryStage) {
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

        // Sökfältet / TopBar är tillfälligt bortkommenterat under Sprint 2
        // mainCol.setTop(buildTopBar(router));
        mainCol.setCenter(scroll);

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
     * TopBar med sökfält är tillfälligt bortkommenterat för Sprint 2.
     */
    /*
    private VBox buildTopBar(PageRouter router) {
        javafx.scene.control.TextField searchField = new javafx.scene.control.TextField();
        searchField.getStyleClass().add("search");
        searchField.setPromptText("Search work orders, customers or vehicles…");
        searchField.setPrefWidth(320);
        // Koppla interaktiv sök-dropdown som fälls ut direkt under sökfältet
        com.wac.autocore.ui.components.SearchDropdown.attach(searchField, garage, router);

        searchField.setOnAction(e -> {
            if (router != null) {
                router.smartNavigateForSearch(searchField.getText());
            }
        });

        javafx.scene.layout.HBox topRow = new javafx.scene.layout.HBox(14, searchField);
        topRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        topRow.getStyleClass().add("topbar");

        VBox topBarContainer = new VBox(0, topRow);
        topBarContainer.getStyleClass().add("topbar-container");
        return topBarContainer;
    }
    */

    public static void main(String[] args) {
        launch(args);
    }
}
