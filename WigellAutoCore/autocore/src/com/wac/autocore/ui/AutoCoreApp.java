package com.wac.autocore.ui;

import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.theme.ThemeManager;
import com.wac.autocore.ui.components.TopBarView;
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
 * Modulariserad och lättunderhållen arkitektur:
 * - SidebarView: navigering och sektioner
 * - TopBarView: sökfält och temaväljare med popup-stilsynk
 * - PageRouter: sidbyten och sökkoppling
 * - Views & Components: dedikerade moduler per vy och tabell
 */
public class AutoCoreApp extends Application {

    private final GarageSystem garage = new GarageSystem();

    @Override
    public void start(Stage primaryStage) {
        BorderPane stage = new BorderPane();
        stage.setPadding(new Insets(14));
        stage.getStyleClass().add("stage");

        BorderPane shell = new BorderPane();
        shell.getStyleClass().add("shell");

        VBox pageBox = new VBox(18);
        pageBox.getStyleClass().add("pages");

        ScrollPane scroll = new ScrollPane(pageBox);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        // Initiera navigering och sidhanterare
        PageRouter router = new PageRouter(garage, pageBox);
        SidebarView sidebar = new SidebarView(router::navigate);
        router.setSidebar(sidebar);

        TopBarView topBar = new TopBarView(router::applySearch);

        BorderPane mainCol = new BorderPane();
        mainCol.getStyleClass().add("col");
        mainCol.setTop(topBar.getView());
        mainCol.setCenter(scroll);

        shell.setLeft(sidebar.getView());
        shell.setCenter(mainCol);
        stage.setCenter(shell);

        Scene scene = new Scene(stage, 1280, 800);
        ThemeManager.applyDefault(scene);

        primaryStage.setTitle("Wigell AutoCore");
        primaryStage.setScene(scene);
        primaryStage.show();

        router.navigate("overview");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
