package com.wac.autocore.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * Reads the files that follow the program: themes, texts and the sample data. They are on the
 * classpath, so it does not matter where the program is started from.
 */
public final class Resources {

    private static final Logger log = LoggerFactory.getLogger(Resources.class);

    private Resources() {}

    /** The resource on the classpath, or null. A leading slash means from the root. */
    public static URL find(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        URL url = Resources.class.getResource(path);

        // Two forms and no more. A third attempt with ClassLoader.getSystemResource would search
        // the same classpath as the line above, only once more.
        if (url == null) {
            String clean = path.startsWith("/") ? path.substring(1) : path;
            url = Resources.class.getClassLoader().getResource(clean);
        }

        return url;
    }

    /**
     * The file as it stands, empty when it cannot be read.
     */
    public static String text(URL url) {
        if (url == null) {
            return "";
        }

        StringBuilder out = new StringBuilder();
        char[] buffer = new char[4096];
        try (Reader reader = new InputStreamReader(url.openStream(), StandardCharsets.UTF_8)) {
            int read = reader.read(buffer);
            while (read != -1) {
                out.append(buffer, 0, read);
                read = reader.read(buffer);
            }
        } catch (Exception e) {
            log.warn("Kunde inte läsa {}", url, e);
        }
        return out.toString();
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
            log.warn("Kunde inte läsa {}", url, e);
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
