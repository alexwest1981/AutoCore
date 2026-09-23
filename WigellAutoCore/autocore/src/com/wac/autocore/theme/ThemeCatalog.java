package com.wac.autocore.theme;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Generated theme catalogue. Regenerate with themer.py convert.
 * Each Theme knows where its JavaFX stylesheet lives on the classpath.
 */
public final class ThemeCatalog {

    public static final String DEFAULT_SLUG = "emerald";

    public static final class Theme {
        public final String slug;
        public final String name;
        public final String stylesheet;
        public final boolean dark;
        public final String accent;
        public Theme(String slug, String name, String stylesheet, boolean dark, String accent) {
            this.slug = slug; this.name = name; this.stylesheet = stylesheet;
            this.dark = dark; this.accent = accent;
        }
    }

    private static final List<Theme> THEMES = new ArrayList<Theme>();
    static {
        Collections.addAll(THEMES,
            new Theme("emerald", "Emerald", "/com/wac/autocore/theme/themes/emerald/emerald.css", false, "#159e72"));
    }

    public static List<Theme> all() {
        return Collections.unmodifiableList(THEMES);
    }

    public static Theme bySlug(String slug) {
        for (Theme t : THEMES) {
            if (t.slug.equals(slug)) return t;
        }
        return THEMES.isEmpty() ? null : THEMES.get(0);
    }
}
