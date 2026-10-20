package com.wac.autocore.util;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * All texts, both the interface's and the seed data's, read from the same files.
 * The only place a language is loaded and held.
 */
public class Texts {

    public static final String LANG_EN = "en";
    public static final String LANG_SV = "sv";
    public static final String DEFAULT_LANG = LANG_EN;

    private static final String RESOURCE_PATH = "/com/wac/autocore/i18n/";

    private static String currentLanguage = DEFAULT_LANG;
    private static final Map<String, String> active = new HashMap<String, String>();
    private static final Map<String, String> fallback = new HashMap<String, String>();

    static {
        active.putAll(load(DEFAULT_LANG));
        fallback.putAll(active);
    }

    private Texts() {}

    /** The text for the key. When it is missing the key itself shows, so a gap is visible. */
    public static synchronized String get(String key) {
        if (key == null) {
            return "";
        }
        String value = active.get(key);
        if (value == null) {
            value = fallback.get(key);
        }
        return value != null ? value : key;
    }

    /** Switches to a language. An unknown language becomes the default. */
    public static synchronized void use(String lang) {
        String normalized = lang == null ? DEFAULT_LANG : lang.trim().toLowerCase();
        if (!LANG_SV.equals(normalized) && !LANG_EN.equals(normalized)) {
            normalized = DEFAULT_LANG;
        }
        currentLanguage = normalized;
        active.clear();
        active.putAll(load(normalized));
    }
    public static synchronized String getLanguage() {
        return currentLanguage;
    }

    /** Reads the texts for one language from its file. */
    public static Map<String, String> load(String lang) {
        URL url = Resources.find(RESOURCE_PATH + lang + ".json");
        if (url == null) {
            System.err.println("Varning: Kunde inte hitta språkfilen för " + lang);
            return Collections.emptyMap();
        }
        return Resources.json(url);
    }
}
