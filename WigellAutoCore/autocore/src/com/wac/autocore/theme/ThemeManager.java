package com.wac.autocore.theme;

import javafx.scene.Scene;
import javafx.scene.Parent;

/**
 * Applies a generated theme's stylesheet to a Scene.
 *
 * Usage (Java):
 *     Scene scene = new Scene(root);
 *     ThemeManager.applyDefault(scene);       // the locked-in theme
 *     // or: ThemeManager.apply(scene, "slug");
 *
 * The scene root should carry the style class "root" (add it once):
 *     root.getStyleClass().add("root");
 */
public final class ThemeManager {

    private ThemeManager() {}

    public static void applyDefault(Scene scene) {
        apply(scene, ThemeCatalog.DEFAULT_SLUG);
    }

    public static void apply(Scene scene, String slug) {
        if (scene == null) return;
        ThemeCatalog.Theme theme = ThemeCatalog.bySlug(slug);
        if (theme == null) return;
        scene.getStylesheets().clear();
        scene.getStylesheets().add(ThemeManager.class.getResource(theme.stylesheet).toExternalForm());
        Parent root = scene.getRoot();
        if (root != null && !root.getStyleClass().contains("root")) {
            root.getStyleClass().add("root");
        }
    }
}
