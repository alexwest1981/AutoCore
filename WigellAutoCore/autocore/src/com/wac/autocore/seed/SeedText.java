package com.wac.autocore.seed;

import com.wac.autocore.util.Resources;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** The texts in the seed data. The interface's texts live in i18n. */
public final class SeedText {

    public static final String LANG_EN = "en";
    public static final String LANG_SV = "sv";
    public static final String DEFAULT_LANG = LANG_EN;

    /** Marks a stored value as a key in this dictionary instead of finished text. */
    public static final String PREFIX = "seed.";

    private static final String RESOURCE_PATH = "/com/wac/autocore/seed/";
    private static final String SOURCE_PATH = "WigellAutoCore/autocore/src/resources/com/wac/autocore/seed/";
    private static final String BUILD_PATH = "out/production/Systemarkitektur/com/wac/autocore/seed/";

    private static String currentLanguage = DEFAULT_LANG;
    private static final Map<String, String> activeDictionary = new HashMap<String, String>();
    private static final Map<String, String> fallbackDictionary = new HashMap<String, String>();

    static {
        loadLanguage(DEFAULT_LANG);
        synchronized (activeDictionary) {
            fallbackDictionary.putAll(activeDictionary);
        }
    }

    private SeedText() {}

    /** The text for a value: the translation if the value is a key, otherwise the value itself. */
    public static String resolve(String value) {
        if (value == null) {
            return null;
        }
        if (!isKey(value)) {
            return value;
        }
        return get(value);
    }

    /** The translation for the key. When it is missing, the key is shown. */
    public static String get(String key) {
        if (key == null) {
            return "";
        }
        synchronized (activeDictionary) {
            String value = activeDictionary.get(key);
            if (value != null) {
                return value;
            }
            value = fallbackDictionary.get(key);
            if (value != null) {
                return value;
            }
        }
        return key;
    }

    /** True when the stored value is a key in this dictionary. */
    public static boolean isKey(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    /** Sets the language for the seed data. Called by I18n, so the dictionaries stay in step. */
    public static synchronized void setLanguage(String lang) {
        String normalized = lang == null ? DEFAULT_LANG : lang.trim().toLowerCase();
        if (!LANG_SV.equals(normalized) && !LANG_EN.equals(normalized)) {
            normalized = DEFAULT_LANG;
        }
        currentLanguage = normalized;
        loadLanguage(normalized);
    }

    public static synchronized String getLanguage() {
        return currentLanguage;
    }

    /** Reads the dictionary for a language. */
    public static Map<String, String> loadDictionary(String lang) {
        String path = RESOURCE_PATH + lang + ".json";
        URL url = Resources.find(path, SOURCE_PATH + lang + ".json", BUILD_PATH + lang + ".json");
        if (url == null) {
            System.err.println("Varning: Kunde inte hitta seed-ordboken " + path);
            return Collections.emptyMap();
        }
        return Resources.json(url);
    }

    private static void loadLanguage(String lang) {
        Map<String, String> loaded = loadDictionary(lang);
        synchronized (activeDictionary) {
            activeDictionary.clear();
            activeDictionary.putAll(loaded);
        }
    }

}
