package com.wac.autocore.ui;

import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.theme.ThemeCatalog;
import com.wac.autocore.theme.ThemeCatalog.Theme;
import com.wac.autocore.theme.ThemeManager;
import com.wac.autocore.ui.navigation.PageRouter;
import com.wac.autocore.ui.navigation.SidebarView;
import com.wac.autocore.ui.navigation.TopNavView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * JavaFX GUI for Wigell AutoCore.
 *
 * Modulariserad och lättunderhållen arkitektur:
 * - SidebarView: navigering och sektioner (Sidebar-läge)
 * - TopNavView: horisontell toppnavigering (Top bar-läge)
 * - buildTopBar: inbyggd toppmeny (sökfält, layout-toggle och temaväljare)
 * - PageRouter: sidbyten och sökkoppling
 * - Views & Components: dedikerade moduler per vy och tabell
 */
public class AutoCoreApp extends Application {

    public enum NavigationMode {
        SIDEBAR,
        TOPBAR
    }

    private final GarageSystem garage = new GarageSystem();
    private NavigationMode navMode = NavigationMode.SIDEBAR;

    @Override
    public void start(Stage primaryStage) {
        BorderPane stage = new BorderPane();
        stage.setPadding(new Insets(14));
        stage.getStyleClass().addAll("root", "stage");

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
        TopNavView topNav = new TopNavView(router::navigate);
        router.setSidebar(sidebar);
        router.setTopNav(topNav);

        BorderPane mainCol = new BorderPane();
        mainCol.getStyleClass().add("col");

        // TopBar med sökfält, layout-växlare och temaväljare
        mainCol.setTop(buildTopBar(router, sidebar, topNav, shell, mainCol));
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
     * Bygger applikationens TopBar med sökfält, layout-toggle (Sidebar / Top bar) och temaväljare.
     */
    private VBox buildTopBar(PageRouter router, SidebarView sidebar, TopNavView topNav, BorderPane shell, BorderPane mainCol) {
        HBox brandRow = buildBrandMark(router);

        TextField searchField = new TextField();
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

        Button layoutToggleBtn = new Button();
        layoutToggleBtn.getStyleClass().add("layout-toggle");

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

        HBox topRow = new HBox(14, brandRow, searchField, spacer, layoutToggleBtn, themeBox);
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.getStyleClass().add("topbar");

        VBox topBarContainer = new VBox(0, topRow, topNav.getView());
        topBarContainer.getStyleClass().add("topbar-container");

        Runnable updateMode = () -> {
            if (navMode == NavigationMode.SIDEBAR) {
                shell.setLeft(sidebar.getView());
                brandRow.setVisible(false);
                brandRow.setManaged(false);
                topNav.getView().setVisible(false);
                topNav.getView().setManaged(false);
                layoutToggleBtn.setText("Sidebar \u25E7");
                layoutToggleBtn.setTooltip(new Tooltip("Klicka för att växla till Top bar"));
                mainCol.getStyleClass().remove("topbar-mode");
                topBarContainer.getStyleClass().remove("topbar-mode");
            } else {
                shell.setLeft(null);
                brandRow.setVisible(true);
                brandRow.setManaged(true);
                topNav.getView().setVisible(true);
                topNav.getView().setManaged(true);
                layoutToggleBtn.setText("Top bar \u25EB");
                layoutToggleBtn.setTooltip(new Tooltip("Klicka för att växla till Sidebar"));
                if (!mainCol.getStyleClass().contains("topbar-mode")) {
                    mainCol.getStyleClass().add("topbar-mode");
                }
                if (!topBarContainer.getStyleClass().contains("topbar-mode")) {
                    topBarContainer.getStyleClass().add("topbar-mode");
                }
            }
        };

        layoutToggleBtn.setOnAction(e -> {
            navMode = (navMode == NavigationMode.SIDEBAR) ? NavigationMode.TOPBAR : NavigationMode.SIDEBAR;
            updateMode.run();
        });

        updateMode.run();
        return topBarContainer;
    }

    private HBox buildBrandMark(PageRouter router) {
        StackPane mark = new StackPane();
        mark.getStyleClass().add("brand-mark");
        mark.setPrefSize(34, 34);
        Label letter = new Label("AC");
        letter.getStyleClass().add("letter");
        mark.getChildren().add(letter);

        VBox brandTitles = new VBox(0);
        Label brand = new Label("AutoCore");
        brand.getStyleClass().add("brand-title");
        Label brandSub = new Label("Workshop System");
        brandSub.getStyleClass().add("brand-sub");
        brandTitles.getChildren().addAll(brand, brandSub);

        HBox brandRow = new HBox(8, mark, brandTitles);
        brandRow.getStyleClass().add("brand-row");
        brandRow.setAlignment(Pos.CENTER_LEFT);
        brandRow.setCursor(Cursor.HAND);
        brandRow.setOnMouseClicked(e -> {
            if (router != null) {
                router.navigate("overview");
            }
        });
        return brandRow;
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
                        if (popScene.getRoot() != null && !popScene.getRoot().getStyleClass().contains("root")) {
                            popScene.getRoot().getStyleClass().add("root");
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
