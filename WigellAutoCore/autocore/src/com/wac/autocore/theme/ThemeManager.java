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
     * Det delade komponentlagret. Färgfilen (azure.css m.fl.) sätter tokens och
     * färg; components.css stylar element som stapeldiagram, donut, KPI-ikon,
     * snabbåtgärder, tabell, flikar och skuggor — element som designerna använder
     * men som färgfilerna inte täcker.
     *
     * Ligger här och inte i en enskild app, så varje app som använder ett tema
     * får lagret automatiskt. Det läggs FÖRST i listan, så temats egna regler
     * vinner där de överlappar.
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
            System.err.println("[ThemeManager] Varning: Kunde inte hitta stilmall för tema '" + slug + "': " + theme.stylesheet);
        }
        Parent root = scene.getRoot();
        if (root != null && !root.getStyleClass().contains("root")) {
            root.getStyleClass().add("root");
        }
    }
}
