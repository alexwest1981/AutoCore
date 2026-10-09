package com.wac.autocore.ui.i18n;

import com.wac.autocore.util.Resources;

import java.net.URL;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** The translations. Reads its own JSON files, no external dependencies. */
public final class I18n {

    public static final String LANG_EN = "en";
    public static final String LANG_SV = "sv";
    public static final String DEFAULT_LANG = LANG_EN;

    private static String currentLanguage = DEFAULT_LANG;
    private static final Map<String, String> activeDictionary = new HashMap<>();
    private static final Map<String, String> fallbackDictionary = new HashMap<>();
    private static final List<Consumer<String>> listeners = new CopyOnWriteArrayList<>();

    static {
        loadLanguage(DEFAULT_LANG);
        fallbackDictionary.putAll(activeDictionary);
        com.wac.autocore.seed.SeedText.setLanguage(DEFAULT_LANG);
    }

    private I18n() {}

    /** The text for the key. If it is missing the key itself shows, so a gap is visible. */
    public static String get(String key) {
        if (key == null) return "";
        synchronized (activeDictionary) {
            String val = activeDictionary.get(key);
            if (val != null) return val;
            val = fallbackDictionary.get(key);
            if (val != null) return val;
        }
        return key;
    }

    /** The text with MessageFormat, for {0}, {1} and so on. */
    public static String get(String key, Object... args) {
        String pattern = get(key);
        if (args == null || args.length == 0) {
            return pattern;
        }
        try {
            return MessageFormat.format(pattern, args);
        } catch (Exception e) {
            return pattern;
        }
    }

    /** Switches language and tells the listeners. */
    public static synchronized void setLanguage(String lang) {
        if (lang == null || lang.trim().isEmpty()) {
            lang = DEFAULT_LANG;
        }
        String normalized = lang.trim().toLowerCase();
        if (!LANG_SV.equals(normalized) && !LANG_EN.equals(normalized)) {
            normalized = DEFAULT_LANG;
        }

        currentLanguage = normalized;
        loadLanguage(normalized);
        com.wac.autocore.seed.SeedText.setLanguage(normalized);

        for (Consumer<String> listener : listeners) {
            try {
                listener.accept(currentLanguage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static synchronized String getLanguage() {
        return currentLanguage;
    }

    public static boolean isSwedish() {
        return LANG_SV.equalsIgnoreCase(getLanguage());
    }

    public static void addListener(Consumer<String> listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /** Loads the dictionary for a language. */
    public static Map<String, String> loadDictionary(String lang) {
        String resourcePath = "/com/wac/autocore/i18n/" + lang + ".json";
        URL url = Resources.find(resourcePath, i18nSourcePath(lang), i18nBuildPath(lang));
        if (url == null) {
            System.err.println("Varning: Kunde inte hitta språkfil " + resourcePath);
            return Collections.emptyMap();
        }
        return Resources.json(url);
    }

    /** The file in the source tree, for a run started from the project folder. */
    private static String i18nSourcePath(String lang) {
        return "WigellAutoCore/autocore/src/resources/com/wac/autocore/i18n/" + lang + ".json";
    }

    /** The file IntelliJ writes to when the project is built there. */
    private static String i18nBuildPath(String lang) {
        return "out/production/Systemarkitektur/com/wac/autocore/i18n/" + lang + ".json";
    }

    private static void loadLanguage(String lang) {
        Map<String, String> loaded = loadDictionary(lang);
        synchronized (activeDictionary) {
            activeDictionary.clear();
            activeDictionary.putAll(loaded);
        }
    }
}
