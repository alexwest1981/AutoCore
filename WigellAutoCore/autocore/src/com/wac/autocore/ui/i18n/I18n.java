package com.wac.autocore.ui.i18n;

import com.wac.autocore.util.Texts;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** The interface's view of the texts: placeholders like {0} and listeners for language switches. */
public final class I18n {

    public static final String LANG_EN = Texts.LANG_EN;
    public static final String LANG_SV = Texts.LANG_SV;
    public static final String DEFAULT_LANG = Texts.DEFAULT_LANG;


    private static final List<Consumer<String>> listeners = new CopyOnWriteArrayList<>();



    private I18n() {}

    /** The text for the key. If it is missing the key itself shows, so a gap is visible. */
    public static String get(String key) {
        return Texts.get(key);
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
        Texts.use(lang);
        for (Consumer<String> listener : listeners) {
            try {
                listener.accept(Texts.getLanguage());
            } catch (Exception e) {
                System.err.println("Could not tell a listener about language '"
                        + Texts.getLanguage() + "': " + e.getMessage());
            }
        }
    }

    public static String getLanguage() {
        return Texts.getLanguage();
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
        return Texts.load(lang);
    }

}
