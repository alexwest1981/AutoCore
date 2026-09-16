package com.wac.autocore.theme;

import java.net.URL;

import javafx.scene.Scene;
import javafx.scene.Parent;

/**
 * Applies a generated theme's stylesheet to a Scene.
 *
 * Usage (Java):
 *     Scene scene = new Scene(root);
 *     ThemeManager.applyDefault(scene);       // applies the default theme
 *     // or: ThemeManager.apply(scene, "slug");
 *
 * Available slugs: "default" (plain JavaFX), "light", "dark",
 *                  "azure", "classic", "emerald", "night", "volt".
 *
 * The scene root should carry the style class "root" (add it once):
 *     root.getStyleClass().add("root");
 */
public final class ThemeManager {

    private ThemeManager() {}

    /** The most recently styled Scene — used by dialogs to inherit the active theme. */
    private static Scene currentScene;

    /** Returns the Scene that last had a theme applied, or {@code null} if none yet. */
    public static Scene getCurrentScene() {
        return currentScene;
    }

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
     *
     * NOTE: components.css is intentionally skipped for the "default" theme so
     * that JavaFX's built-in Modena stylesheet is left completely intact.
     */
    private static final String COMPONENTS = "/com/wac/autocore/theme/components.css";

    /** Slug for the plain-JavaFX theme — no custom CSS is applied at all. */
    private static final String DEFAULT_PLAIN_SLUG = "default";

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
        currentScene = scene;
        if (scene == null) return;
        ThemeCatalog.Theme theme = ThemeCatalog.bySlug(slug);
        if (theme == null) return;

        // Always clear existing stylesheets first.
        scene.getStylesheets().clear();

        if (DEFAULT_PLAIN_SLUG.equals(slug)) {
            // "default" — skip components.css (which uses -wac-* colour tokens
            // that Modena doesn't define) but do load the layout-only CSS so the
            // sidebar, topbar and page canvas keep their correct spacing.
            URL themeUrl = resolveResource(theme.stylesheet);
            if (themeUrl != null) {
                scene.getStylesheets().add(themeUrl.toExternalForm());
            }
            // Do NOT add the "root" style class: Modena targets the root pane
            // via its own internal selectors, and adding "root" can interfere.
            return;
        }

        // All other themes: load the shared component layer first, then the
        // theme-specific colour / token file on top.
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
