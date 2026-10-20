package com.wac.autocore.config;

import com.wac.autocore.data.Db;

/**
 * The one rule for where the storage lives, for the programs that run without a context.
 *
 * Spring reads the same property through {@link DatabaseConfig}; a proof that starts on its own
 * asks here instead, so a run against a copy is the same setting either way.
 */
public final class DatabasePath {

    /** The property both the application and the proofs read. */
    public static final String PROPERTY = "autocore.db.path";

    private DatabasePath() {}

    /** The property first, then the first argument, then the application's own file. */
    public static String resolve(String[] args) {
        String property = System.getProperty(PROPERTY);
        if (property != null && !property.trim().isEmpty()) {
            return property.trim();
        }
        return args.length > 0 ? args[0] : Db.DEFAULT_PATH;
    }
}
