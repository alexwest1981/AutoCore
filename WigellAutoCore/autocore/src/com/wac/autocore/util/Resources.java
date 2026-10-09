package com.wac.autocore.util;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Reads a text file as keys and values. The caller says where the file can be, because the
 * project looks different in the editor and in a finished build.
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
     * The json file as keys and values, empty when it cannot be read. org.json does the reading,
     * so the file may be written however it wants.
     */
    public static Map<String, String> json(URL url) {
        Map<String, String> result = new HashMap<String, String>();
        if (url == null) {
            return Collections.unmodifiableMap(result);
        }

        try (Reader reader = new InputStreamReader(url.openStream(), StandardCharsets.UTF_8)) {
            collect(new JSONObject(new JSONTokener(reader)), "", result);
        } catch (Exception e) {
            System.err.println("Kunde inte läsa " + url + ": " + e.getMessage());
        }
        return Collections.unmodifiableMap(result);
    }

    /** Stores every value together with the key it sits under. */
    private static void collect(JSONObject object, String prefix, Map<String, String> out) {
        String[] keys = JSONObject.getNames(object);
        if (keys == null) {
            return;
        }
        for (String key : keys) {
            Object value = object.get(key);
            if (value instanceof JSONObject) {
                collect((JSONObject) value, prefix + key + ".", out);
            } else {
                out.put(prefix + key, String.valueOf(value));
            }
        }
    }
}
