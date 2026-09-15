package com.wac.autocore.theme;

import java.net.URL;

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

    /**
     * The shared component layer. The colour file (azure.css etc.) sets tokens
     * and colours; components.css styles elements such as bar charts, donuts,
     * KPI icons, quick-actions, tables, tabs and shadows — elements used by the
     * themes but not covered by the colour files.
     *
     * Kept here rather than in a single app so that every app using a theme
     * receives the layer automatically. It is placed FIRST in the list so that
     * the theme's own rules win where they overlap.
     */
    private static final String COMPONENTS = "/com/wac/autocore/theme/components.css";

    private static URL resolveResource(String path) {
        if (path == null) return null;
        URL url = ThemeManager.class.getResource(path);
        if (url != null) return url;
        String cleanPath = path.startsWith("/") ? path.substring(1) : path;
        url = ThemeManager.class.getClassLoader().getResource(cleanPath);
        if (url != null) return url;
        java.io.File file = new java.io.File("WigellAutoCore/autocore/src/resources" + (path.startsWith("/") ? path : "/" + path));
        if (file.exists()) {
            try {
                return file.toURI().toURL();
            } catch (java.net.MalformedURLException ignored) {}
        }
        return null;
    }

    public static void apply(Scene scene, String slug) {
        if (scene == null) return;
        ThemeCatalog.Theme theme = ThemeCatalog.bySlug(slug);
        if (theme == null) return;
        scene.getStylesheets().clear();
        URL components = resolveResource(COMPONENTS);
        if (components != null) {
            scene.getStylesheets().add(components.toExternalForm());
        }
        URL themeUrl = resolveResource(theme.stylesheet);
        if (themeUrl != null) {
            scene.getStylesheets().add(themeUrl.toExternalForm());
        } else {
            System.err.println("[ThemeManager] Warning: Could not find stylesheet for theme '" + slug + "': " + theme.stylesheet);
        }
        Parent root = scene.getRoot();
        if (root != null && !root.getStyleClass().contains("root")) {
            root.getStyleClass().add("root");
        }
    }
}
