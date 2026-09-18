package com.wac.autocore.ui;

import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.theme.ThemeCatalog;
import com.wac.autocore.theme.ThemeCatalog.Theme;
import com.wac.autocore.theme.ThemeManager;
import com.wac.autocore.ui.navigation.PageRouter;
import com.wac.autocore.ui.navigation.SidebarView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * JavaFX GUI for Wigell AutoCore.
 *
 * Modulariserad och lättunderhållen arkitektur:
 * - SidebarView: navigering och sektioner
 * - buildTopBar: inbyggd toppmeny (sökfält och temaväljare) som enkelt kan avaktiveras
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

        BorderPane mainCol = new BorderPane();
        mainCol.getStyleClass().add("col");

        // TopBar med sökfält och temaväljare.
        // För att avaktivera: kommentera bara bort raden nedan!
        mainCol.setTop(buildTopBar(router));

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

    /**
     * Bygger applikationens TopBar med sökfält och temaväljare.
     * Skapad direkt i JFX-koden så den enkelt kan avaktiveras eller tas bort.
     */
    private HBox buildTopBar(PageRouter router) {
        TextField searchField = new TextField();
        searchField.getStyleClass().add("search");
        searchField.setPromptText("Search work orders, customers or vehicles…");
        searchField.setPrefWidth(320);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (router != null) {
                router.applySearch(newVal);
            }
        });
        searchField.setOnAction(e -> {
            if (router != null) {
                router.smartNavigateForSearch(searchField.getText());
            }
        });

        ComboBox<Theme> themeBox = new ComboBox<Theme>();
        themeBox.getStyleClass().add("theme-pick");
        themeBox.setItems(FXCollections.observableArrayList(ThemeCatalog.all()));
        themeBox.getSelectionModel().select(ThemeCatalog.bySlug(ThemeCatalog.DEFAULT_SLUG));

        themeBox.setCellFactory(param -> new ListCell<Theme>() {
            @Override
            protected void updateItem(Theme t, boolean empty) {
                super.updateItem(t, empty);
                setText(t == null || empty ? null : t.name);
            }
        });
        themeBox.setButtonCell(new ListCell<Theme>() {
            @Override
            protected void updateItem(Theme t, boolean empty) {
                super.updateItem(t, empty);
                setText(t == null || empty ? null : t.name);
            }
        });

        themeBox.valueProperty().addListener((obs, oldT, newT) -> {
            Scene scene = themeBox.getScene();
            if (scene != null && newT != null) {
                ThemeManager.apply(scene, newT.slug);
                Platform.runLater(() -> syncComboPopup(themeBox));
            }
        });
        themeBox.setOnShowing(e -> syncComboPopup(themeBox));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox container = new HBox(14, searchField, spacer, themeBox);
        container.setAlignment(Pos.CENTER_LEFT);
        container.getStyleClass().add("topbar");
        return container;
    }

    /**
     * JavaFX ComboBox-popupen är ett separat PopupWindow med egen Scene.
     * Ser till att popupen ärver appscenens stylesheets samt att rotnoden har klassen 'root'
     * så att modena/WAC-tokens (-fx-accent m.fl.) hittas utan CSS-varningar.
     */
    private static void syncComboPopup(ComboBox<?> box) {
        if (box == null || box.getSkin() == null || box.getScene() == null) {
            return;
        }
        Scene appScene = box.getScene();
        for (Class<?> c = box.getSkin().getClass(); c != null; c = c.getSuperclass()) {
            try {
                java.lang.reflect.Field f = c.getDeclaredField("popup");
                f.setAccessible(true);
                Object popupObj = f.get(box.getSkin());
                if (popupObj instanceof javafx.stage.Window) {
                    Scene popScene = ((javafx.stage.Window) popupObj).getScene();
                    if (popScene != null) {
                        popScene.getStylesheets().setAll(appScene.getStylesheets());
                        if (popScene.getRoot() != null) {
                            boolean hasRoot = appScene.getRoot() != null && appScene.getRoot().getStyleClass().contains("root");
                            if (hasRoot && !popScene.getRoot().getStyleClass().contains("root")) {
                                popScene.getRoot().getStyleClass().add("root");
                            } else if (!hasRoot) {
                                popScene.getRoot().getStyleClass().remove("root");
                            }
                        }
                    }
                }
                return;
            } catch (NoSuchFieldException ignored) {
            } catch (Exception e) {
                return;
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
