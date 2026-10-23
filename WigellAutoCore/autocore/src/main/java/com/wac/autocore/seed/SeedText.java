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

    /**
     * The stable part of a stored value: a seed key without its prefix and its last attribute, so
     * {@code seed.mechanic.general_service.specialization} becomes {@code mechanic.general_service}.
     * A value that is not a key is returned as trimmed lowercase text.
     */
    public static String code(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (!isKey(trimmed)) {
            return trimmed.toLowerCase(java.util.Locale.ROOT);
        }
        String withoutPrefix = trimmed.substring(PREFIX.length());
        int lastDot = withoutPrefix.lastIndexOf('.');
        String code = lastDot > 0 ? withoutPrefix.substring(0, lastDot) : withoutPrefix;
        return code.toLowerCase(java.util.Locale.ROOT);
    }

    /** True when the stored value is a key. */
    public static boolean isKey(String value) {
        return value != null && value.startsWith(PREFIX);
    }
}