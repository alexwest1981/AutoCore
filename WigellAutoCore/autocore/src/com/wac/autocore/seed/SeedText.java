package com.wac.autocore.seed;

import com.wac.autocore.util.Texts;

/** The seed data's view of the texts: a stored value is either a key or finished text. */
public final class SeedText {

    /** Marks a stored value as a key instead of finished text. */
    public static final String PREFIX = "seed.";

    private SeedText() {}

    /** The text for a value: the translation if the value is a key, otherwise the value itself. */
    public static String resolve(String value) {
        if (value == null) {
            return null;
        }
        return isKey(value) ? Texts.get(value) : value;
    }

    /** The translation for the key. */
    public static String get(String key) {
        return Texts.get(key);
    }

    /** True when the stored value is a key. */
    public static boolean isKey(String value) {
        return value != null && value.startsWith(PREFIX);
    }
}