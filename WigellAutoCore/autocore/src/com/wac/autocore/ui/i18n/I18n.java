package com.wac.autocore.ui.i18n;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
        if (!DEFAULT_LANG.equals(LANG_EN)) {
            fallbackDictionary.putAll(loadDictionary(LANG_EN));
        } else {
            fallbackDictionary.putAll(activeDictionary);
        }
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
                System.err.println("Could not tell a listener about language '"
                        + currentLanguage + "': " + e.getMessage());
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
        Map<String, String> result = new HashMap<>();
        String resourcePath = "/com/wac/autocore/i18n/" + lang + ".json";
        InputStream in = resolveResourceStream(resourcePath, lang);
        if (in != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                parseJsonObject("", sb.toString().trim(), result);
            } catch (Exception e) {
                System.err.println("Fel vid inläsning av språkfil " + resourcePath + ": " + e.getMessage());
            }
        } else {
            System.err.println("Varning: Kunde inte hitta språkfil " + resourcePath);
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

    private static InputStream resolveResourceStream(String path, String lang) {
        InputStream in = I18n.class.getResourceAsStream(path);
        if (in != null) return in;

        String clean = path.startsWith("/") ? path.substring(1) : path;
        in = I18n.class.getClassLoader().getResourceAsStream(clean);
        if (in != null) return in;

        in = ClassLoader.getSystemResourceAsStream(clean);
        if (in != null) return in;

        File f = new File("WigellAutoCore/autocore/src/resources/com/wac/autocore/i18n/" + lang + ".json");
        if (f.exists()) {
            try {
                return new FileInputStream(f);
            } catch (Exception ignored) {}
        }
        File fOut = new File("out/production/Systemarkitektur/com/wac/autocore/i18n/" + lang + ".json");
        if (fOut.exists()) {
            try {
                return new FileInputStream(fOut);
            } catch (Exception ignored) {}
        }
        return null;
    }

    /** Reads a flat JSON into dot-notated keys: nav.overview. */
    static void parseJsonObject(String prefix, String json, Map<String, String> out) {
        if (json == null) return;
        json = json.trim();
        if (!json.startsWith("{") || !json.endsWith("}")) return;
        json = json.substring(1, json.length() - 1).trim();

        int i = 0;
        int len = json.length();

        while (i < len) {
            // Skip whitespace and commas
            while (i < len && (Character.isWhitespace(json.charAt(i)) || json.charAt(i) == ',')) {
                i++;
            }
            if (i >= len) break;

            // Read the key (must start with a quote)
            if (json.charAt(i) != '"') {
                i++;
                continue;
            }
            int keyStart = ++i;
            while (i < len && json.charAt(i) != '"') {
                if (json.charAt(i) == '\\') i++;
                i++;
            }
            String rawKey = json.substring(keyStart, i);
            String key = unescapeJson(rawKey);
            i++; // past the closing quote

            // Find the colon ':'
            while (i < len && json.charAt(i) != ':') i++;
            if (i >= len) break;
            i++; // past the ':'

            // Find the start of the value
            while (i < len && Character.isWhitespace(json.charAt(i))) i++;
            if (i >= len) break;

            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;

            if (json.charAt(i) == '{') {
                // Find the matching brace
                int objStart = i;
                int depth = 0;
                boolean inStr = false;
                while (i < len) {
                    char c = json.charAt(i);
                    if (c == '\\' && inStr) {
                        i += 2;
                        continue;
                    }
                    if (c == '"') {
                        inStr = !inStr;
                    } else if (!inStr) {
                        if (c == '{') depth++;
                        else if (c == '}') {
                            depth--;
                            if (depth == 0) {
                                i++; // include the '}'
                                break;
                            }
                        }
                    }
                    i++;
                }
                String subJson = json.substring(objStart, i);
                parseJsonObject(fullKey, subJson, out);
            } else if (json.charAt(i) == '"') {
                int valStart = ++i;
                while (i < len && json.charAt(i) != '"') {
                    if (json.charAt(i) == '\\') i++;
                    i++;
                }
                String rawVal = json.substring(valStart, i);
                out.put(fullKey, unescapeJson(rawVal));
                i++; // past the closing quote
            } else {
                // Primitive value (number, boolean etc)
                int valStart = i;
                while (i < len && json.charAt(i) != ',' && json.charAt(i) != '}' && !Character.isWhitespace(json.charAt(i))) {
                    i++;
                }
                String rawVal = json.substring(valStart, i).trim();
                out.put(fullKey, rawVal);
            }
        }
    }

    private static String unescapeJson(String s) {
        if (s == null || s.indexOf('\\') == -1) return s;
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(++i);
                switch (next) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'u':
                        if (i + 4 < s.length()) {
                            String hex = s.substring(i + 1, i + 5);
                            try {
                                sb.append((char) Integer.parseInt(hex, 16));
                                i += 4;
                            } catch (NumberFormatException e) {
                                sb.append("\\u");
                            }
                        } else {
                            sb.append("\\u");
                        }
                        break;
                    default:
                        sb.append('\\').append(next);
                        break;
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
