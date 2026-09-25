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

/**
 * Internationalisering (i18n) för AutoCore.
 *
 * Hanterar inläsning av hierarkiska JSON-språkfiler utan externa beroenden (ren Java 8).
 * Stödjer dynamisk språkväxling mellan svenska och engelska med aviseringslyssnare.
 */
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

    /**
     * Hämtar översatt text för angiven nyckel.
     * Faller tillbaka på engelska, och därefter nyckelnamnet självt om översättning saknas.
     */
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

    /**
     * Hämtar översatt text och formaterar med MessageFormat ({0}, {1}, etc.).
     */
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

    /**
     * Byter aktivt språk ("sv" eller "en") och aviserar alla registrerade lyssnare.
     */
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

    public static void removeListener(Consumer<String> listener) {
        listeners.remove(listener);
    }

    /**
     * Läser in ordlistan för ett givet språk (används även vid enhetstester).
     */
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

    /**
     * Enkel rekursiv tolkare som extraherar hierarkiska JSON-objekt till punktnoterade nycklar.
     * T.ex. {"nav": {"overview": "Översikt"}} -> "nav.overview"="Översikt".
     */
    static void parseJsonObject(String prefix, String json, Map<String, String> out) {
        if (json == null) return;
        json = json.trim();
        if (!json.startsWith("{") || !json.endsWith("}")) return;
        json = json.substring(1, json.length() - 1).trim();

        int i = 0;
        int len = json.length();

        while (i < len) {
            // Hoppa över blanksteg och kommatecken
            while (i < len && (Character.isWhitespace(json.charAt(i)) || json.charAt(i) == ',')) {
                i++;
            }
            if (i >= len) break;

            // Läs nyckel (måste starta med citattecken)
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
            i++; // förbi avslutande citattecken

            // Hitta kolon ':'
            while (i < len && json.charAt(i) != ':') i++;
            if (i >= len) break;
            i++; // förbi ':'

            // Hitta start av värde
            while (i < len && Character.isWhitespace(json.charAt(i))) i++;
            if (i >= len) break;

            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;

            if (json.charAt(i) == '{') {
                // Hitta matchande klammerparentes
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
                                i++; // inkludera '}'
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
                i++; // förbi avslutande citattecken
            } else {
                // Primitivt värde (tal, boolean etc)
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
