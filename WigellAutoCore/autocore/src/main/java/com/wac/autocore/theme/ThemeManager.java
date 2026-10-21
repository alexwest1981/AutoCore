package com.wac.autocore.theme;

import com.wac.autocore.util.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javafx.scene.Scene;
import javafx.scene.Parent;

/** Puts the theme's stylesheet on a scene. */
public final class ThemeManager {

    private static final Logger log = LoggerFactory.getLogger(ThemeManager.class);

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

    /** The stylesheet from the classpath. */
    private static URL resolveResource(String path) {
        if (path == null) return null;
        String slashPath = path.startsWith("/") ? path : "/" + path;
        return Resources.find(slashPath);
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
                log.warn("Could not find component stylesheet: {}", part);
            }
        }
        URL themeUrl = resolveResource(theme.stylesheet);
        if (themeUrl != null) {
            sheets.add(themeUrl.toExternalForm());
        } else {
            log.warn("Could not find stylesheet for theme '{}': {}", currentSlug, theme.stylesheet);
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
