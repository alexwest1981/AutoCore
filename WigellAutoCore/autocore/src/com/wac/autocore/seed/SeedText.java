package com.wac.autocore.seed;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dictionary for the seeded demo data.
 *
 * The interface texts live in {@code com/wac/autocore/i18n/<lang>.json} and belong to the widgets.
 * The demo data has its own dictionary, one flat file per language, in
 * {@code com/wac/autocore/seed/<lang>.json}. Rows written by {@link com.wac.autocore.data.SeedData}
 * and by the demo schedule store a key from that dictionary rather than a finished sentence, so the
 * same row can be read in Swedish or English without ever being rewritten.
 *
 * {@link #resolve(String)} turns a stored key into text in the active language and returns every
 * value that is not a key untouched. That pass-through is what lets a description the user typed
 * live in the same column as a seeded one: only values that start with {@link #PREFIX} are looked up.
 */
public final class SeedText {

    public static final String LANG_EN = "en";
    public static final String LANG_SV = "sv";
    public static final String DEFAULT_LANG = LANG_EN;

    /** Marks a stored value as a key in this dictionary instead of finished text. */
    public static final String PREFIX = "seed.";

    private static final String RESOURCE_PATH = "/com/wac/autocore/seed/";
    private static final String SOURCE_PATH = "WigellAutoCore/autocore/src/resources/com/wac/autocore/seed/";
    private static final String BUILD_PATH = "out/production/Systemarkitektur/com/wac/autocore/seed/";

    private static final Pattern ENTRY = Pattern.compile(
            "^\\s*\"((?:[^\"\\\\]|\\\\.)*)\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"\\s*,?\\s*$");

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

    /**
     * Text to show for a stored value: the translation when the value is a key in this dictionary,
     * otherwise the value itself.
     */
    public static String resolve(String value) {
        if (value == null) {
            return null;
        }
        if (!isKey(value)) {
            return value;
        }
        return get(value);
    }

    /**
     * Translation for a key in the active language, falling back to English and finally to the key
     * itself so a missing entry is visible instead of empty.
     */
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

    /**
     * Sets the language of the demo data. Called by {@code I18n} whenever the interface language
     * changes, so the two dictionaries can never point at different languages.
     */
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

    /**
     * Reads the dictionary for one language. Public so the tests can compare the two files.
     */
    public static Map<String, String> loadDictionary(String lang) {
        Map<String, String> result = new HashMap<String, String>();
        String path = RESOURCE_PATH + lang + ".json";
        InputStream in = resolveResourceStream(lang);
        if (in == null) {
            System.err.println("Warning: could not find seed dictionary " + path);
            return Collections.unmodifiableMap(result);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                parseEntry(line, result);
            }
        } catch (Exception e) {
            System.err.println("Error reading seed dictionary " + path + ": " + e.getMessage());
        }
        return Collections.unmodifiableMap(result);
    }

    private static void loadLanguage(String lang) {
        Map<String, String> loaded = loadDictionary(lang);
        synchronized (activeDictionary) {
            activeDictionary.clear();
            activeDictionary.putAll(loaded);
        }
    }

    /**
     * Reads one {@code "key": "value"} line. The seed files are flat, so no nesting is handled.
     */
    private static void parseEntry(String line, Map<String, String> target) {
        if (line == null) {
            return;
        }
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("//") || "{".equals(trimmed) || "}".equals(trimmed)) {
            return;
        }
        Matcher matcher = ENTRY.matcher(trimmed);
        if (!matcher.matches()) {
            System.err.println("Warning: skipped line in seed dictionary: " + trimmed);
            return;
        }
        target.put(unescape(matcher.group(1)), unescape(matcher.group(2)));
    }

    private static String unescape(String raw) {
        StringBuilder out = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c != '\\' || i + 1 >= raw.length()) {
                out.append(c);
                continue;
            }
            char next = raw.charAt(++i);
            switch (next) {
                case 'n':
                    out.append('\n');
                    break;
                case 't':
                    out.append('\t');
                    break;
                default:
                    out.append(next);
                    break;
            }
        }
        return out.toString();
    }

    private static InputStream resolveResourceStream(String lang) {
        String path = RESOURCE_PATH + lang + ".json";
        InputStream in = SeedText.class.getResourceAsStream(path);
        if (in != null) {
            return in;
        }

        String clean = path.startsWith("/") ? path.substring(1) : path;
        in = SeedText.class.getClassLoader().getResourceAsStream(clean);
        if (in != null) {
            return in;
        }

        in = ClassLoader.getSystemResourceAsStream(clean);
        if (in != null) {
            return in;
        }

        return openFile(SOURCE_PATH + lang + ".json", BUILD_PATH + lang + ".json");
    }

    private static InputStream openFile(String sourcePath, String buildPath) {
        File source = new File(sourcePath);
        if (source.exists()) {
            try {
                return new FileInputStream(source);
            } catch (Exception ignored) {
            }
        }
        File built = new File(buildPath);
        if (built.exists()) {
            try {
                return new FileInputStream(built);
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
