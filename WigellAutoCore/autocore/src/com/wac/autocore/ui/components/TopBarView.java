package com.wac.autocore.ui.components;

import com.wac.autocore.theme.ThemeCatalog;
import com.wac.autocore.theme.ThemeCatalog.Theme;
import com.wac.autocore.theme.ThemeManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.function.Consumer;

/**
 * Toppmeny med sökfält och temaväljare.
 */
public class TopBarView {

    private static final String THEME_BOX_STYLE =
            "-fx-background-color: white;" +
            "-fx-border-color: #d1d5db;" +
            "-fx-border-radius: 8;" +
            "-fx-background-radius: 8;" +
            "-fx-border-width: 1;" +
            "-fx-padding: 4px 10px;";

    private final HBox container;
    private final TextField searchField;
    private final ComboBox<Theme> themeBox;

    public TopBarView(Consumer<String> onSearch) {
        searchField = new TextField();
        searchField.getStyleClass().add("search");
        searchField.setPromptText("Search work orders, customers or vehicles…");
        searchField.setPrefWidth(320);
        searchField.textProperty().addListener((obs, old, now) -> {
            if (onSearch != null) {
                onSearch.accept(now);
            }
        });

        themeBox = new ComboBox<Theme>();
        themeBox.getStyleClass().add("theme-pick");
        themeBox.setCellFactory(param -> createThemeCell());
        themeBox.setButtonCell(createThemeCell());
        themeBox.setItems(FXCollections.observableArrayList(ThemeCatalog.all()));
        themeBox.getSelectionModel().select(ThemeCatalog.bySlug(ThemeCatalog.DEFAULT_SLUG));
        themeBox.setStyle(THEME_BOX_STYLE);

        themeBox.valueProperty().addListener((obs, oldT, newT) -> {
            Scene scene = themeBox.getScene();
            if (scene != null && newT != null) {
                ThemeManager.apply(scene, newT.slug);
                themeBox.setStyle(THEME_BOX_STYLE);
                Platform.runLater(() -> syncComboPopup(themeBox));
            }
        });
        themeBox.setOnShowing(e -> syncComboPopup(themeBox));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        container = new HBox(14, searchField, spacer, themeBox);
        container.setAlignment(Pos.CENTER_LEFT);
        container.getStyleClass().add("topbar");
    }

    public HBox getView() {
        return container;
    }

    public TextField getSearchField() {
        return searchField;
    }

    private ListCell<Theme> createThemeCell() {
        return new ListCell<Theme>() {
            @Override
            protected void updateItem(Theme t, boolean empty) {
                super.updateItem(t, empty);
                setText(t == null || empty ? null : t.name);
                setStyle("-fx-text-fill: #111827; -fx-background-color: " +
                        (isSelected() ? "#e5e7eb" : "transparent") + "; -fx-padding: 5 10;");
            }
            @Override
            public void updateSelected(boolean selected) {
                super.updateSelected(selected);
                setStyle("-fx-text-fill: #111827; -fx-background-color: " +
                        (selected ? "#e5e7eb" : "transparent") + "; -fx-padding: 5 10;");
            }
        };
    }

    /**
     * JavaFX ComboBox-popupen är ett separat PopupWindow med egen Scene.
     * Denna metod ser till att popupen ärver stylesheets från huvudscenen.
     */
    public static void syncComboPopup(ComboBox<?> box) {
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
                    }
                }
                return;
            } catch (NoSuchFieldException ignored) {
            } catch (Exception e) {
                System.err.println("[TopBarView] syncComboPopup: " + e);
                return;
            }
        }
    }
}
