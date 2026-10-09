package com.wac.autocore.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * One way to find a resource file and read it as a flat map of dot-notated keys. The candidates
 * come from the caller, because the build and the source tree are laid out differently.
 */
public final class Resources {

    private Resources() {}

    /** The first candidate that can be read, or null. A leading slash means classpath. */
    public static URL find(String... candidates) {
        if (candidates == null) {
            return null;
        }
        for (String path : candidates) {
            if (path == null || path.isEmpty()) {
                continue;
            }

            URL url = Resources.class.getResource(path);

            if (url == null) {
                String clean = path.startsWith("/") ? path.substring(1) : path;
                url = Resources.class.getClassLoader().getResource(clean);
            }
            if (url == null) {
                String clean = path.startsWith("/") ? path.substring(1) : path;
                url = ClassLoader.getSystemResource(clean);
            }
            if (url == null) {
                File file = new File(path);
                if (file.exists()) {
                    try {
                        url = file.toURI().toURL();
                    } catch (Exception ignored) {
                    }
                }
            }

            if (url != null) {
                return url;
            }
        }
        return null;
    }

    /**
     * The json file as dot-notated keys, empty when it cannot be read. The reader is hand-written
     * because the project ships no json library.
     */
    public static Map<String, String> json(URL url) {
        Map<String, String> result = new HashMap<String, String>();
        if (url == null) {
            return Collections.unmodifiableMap(result);
        }
        try (InputStream in = url.openStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            parseJsonObject("", sb.toString().trim(), result);
        } catch (Exception e) {
            System.err.println("Kunde inte läsa " + url + ": " + e.getMessage());
        }
        return Collections.unmodifiableMap(result);
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
            
            while (i < len && (Character.isWhitespace(json.charAt(i)) || json.charAt(i) == ',')) {
                i++;
            }
            if (i >= len) break;

            
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
            i++; 

            
            while (i < len && json.charAt(i) != ':') i++;
            if (i >= len) break;
            i++; 

            
            while (i < len && Character.isWhitespace(json.charAt(i))) i++;
            if (i >= len) break;

            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;

            if (json.charAt(i) == '{') {
                
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
                                i++; 
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
                i++;
            } else {
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
