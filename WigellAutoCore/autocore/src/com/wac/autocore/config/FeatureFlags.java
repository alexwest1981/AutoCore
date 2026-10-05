package com.wac.autocore.config;

import java.io.InputStream;
import java.util.Properties;

/**
 * Funktionsväxlar för utveckling: läser {@code config/features.properties} så en funktion kan
 * slås av och på utan att koden rörs.
 *
 * Saknad fil eller saknad nyckel betyder av, och det skrivs till stderr: en växel som står fel
 * ska synas i loggen i stället för att tigas ihjäl, och en nyckel som är felstavad får inte
 * råka sätta på något som är på väg bort.
 *
 * En enskild körning kan överrida en växel med systemegenskapen {@code -Dautocore.features.<namn>}
 * utan att filen ändras.
 */
public final class FeatureFlags {

    private static final String PATH = "/com/wac/autocore/config/features.properties";
    private static final String OVERRIDE_PREFIX = "autocore.features.";

    private static Properties flags;

    private FeatureFlags() {
    }

    /** true bara om växeln finns och står som true. */
    public static boolean isEnabled(String name) {
        String override = System.getProperty(OVERRIDE_PREFIX + name);
        if (override != null) {
            return Boolean.parseBoolean(override.trim());
        }
        Properties loaded = load();
        if (loaded == null) {
            return false;
        }
        return Boolean.parseBoolean(loaded.getProperty(name, "false").trim());
    }

    private static synchronized Properties load() {
        if (flags != null) {
            return flags;
        }
        InputStream in = FeatureFlags.class.getResourceAsStream(PATH);
        if (in == null) {
            System.err.println("FeatureFlags: hittar inte " + PATH + " - alla växlar är av.");
            return null;
        }
        try {
            Properties props = new Properties();
            props.load(in);
            flags = props;
        } catch (Exception e) {
            System.err.println("FeatureFlags: kunde inte läsa " + PATH + ": " + e);
            return null;
        } finally {
            try {
                in.close();
            } catch (Exception ignored) {
                // Stängningen är inte värd att fälla körningen på.
            }
        }
        return flags;
    }
}
