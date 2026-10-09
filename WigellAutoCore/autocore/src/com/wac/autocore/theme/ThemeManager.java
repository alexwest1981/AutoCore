package com.wac.autocore.theme;

import com.wac.autocore.util.Resources;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javafx.scene.Scene;
import javafx.scene.Parent;

/** Puts the theme's stylesheet on a scene. */
public final class ThemeManager {

    private ThemeManager() {}

    /** The most recently styled Scene — used by dialogs to inherit the active theme. */
    private static Scene currentScene;
    private static String currentSlug = ThemeCatalog.DEFAULT_SLUG;

    /** Returns the Scene that last had a theme applied, or {@code null} if none yet. */
    public static Scene getCurrentScene() {
        return currentScene;
    }

    /** The component layer, one file per area. The order is the one the rules had, do not touch it. */
    private static final String[] COMPONENTS = {
        "/com/wac/autocore/theme/components.css",
        "/com/wac/autocore/theme/dashboard.css",
        "/com/wac/autocore/theme/tables.css",
        "/com/wac/autocore/theme/controls.css",
        "/com/wac/autocore/theme/layout.css",
        "/com/wac/autocore/theme/a11y.css",
        "/com/wac/autocore/theme/kanban.css",
        "/com/wac/autocore/theme/kanban-cards.css",
        "/com/wac/autocore/theme/multiselect.css",
    };

    /** The stylesheet from the classpath, or straight from the source tree during development. */
    private static URL resolveResource(String path) {
        if (path == null) return null;
        String slashPath = path.startsWith("/") ? path : "/" + path;
        return Resources.find(path, "WigellAutoCore/autocore/src/resources" + slashPath);
    }

    public static void apply(Scene scene, String slug) {
        currentScene = scene;
        currentSlug = slug != null ? slug : ThemeCatalog.DEFAULT_SLUG;
        if (scene == null) return;
        ThemeCatalog.Theme theme = ThemeCatalog.bySlug(slug);
        if (theme == null) return;

        List<String> sheets = new ArrayList<String>();
        for (String part : COMPONENTS) {
            URL url = resolveResource(part);
            if (url != null) {
                sheets.add(url.toExternalForm());
            } else {
                System.err.println("[ThemeManager] Warning: Could not find component stylesheet: " + part);
            }
        }
        URL themeUrl = resolveResource(theme.stylesheet);
        if (themeUrl != null) {
            sheets.add(themeUrl.toExternalForm());
        } else {
            System.err.println("[ThemeManager] Warning: Could not find stylesheet for theme '" + currentSlug + "': " + theme.stylesheet);
        }

        // Apply all stylesheets atomically in a single operation so JavaFX doesn't
        // trigger intermediate rendering passes with missing tokens.
        scene.getStylesheets().setAll(sheets);

        Parent root = scene.getRoot();
        if (root != null && !root.getStyleClass().contains("root")) {
            root.getStyleClass().add("root");
        }
    }
}
