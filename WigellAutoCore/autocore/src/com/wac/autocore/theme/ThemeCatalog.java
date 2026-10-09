package com.wac.autocore.theme;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The theme catalogue. Rebuilt with themer.py convert. */
public final class ThemeCatalog {

    public static final String DEFAULT_SLUG = "emerald";

    public static final class Theme {
        public final String slug;
        public final String stylesheet;
        public Theme(String slug, String stylesheet) {
            this.slug = slug; this.stylesheet = stylesheet;
        }
    }

    private static final List<Theme> THEMES = new ArrayList<Theme>();
    static {
        Collections.addAll(THEMES,
            new Theme("emerald", "/com/wac/autocore/theme/themes/emerald/emerald.css"));
    }

    public static Theme bySlug(String slug) {
        for (Theme t : THEMES) {
            if (t.slug.equals(slug)) return t;
        }
        return THEMES.isEmpty() ? null : THEMES.get(0);
    }
}
