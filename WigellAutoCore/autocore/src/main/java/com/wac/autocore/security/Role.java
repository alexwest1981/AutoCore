package com.wac.autocore.security;

import java.util.Locale;

/** The three levels the order asks for. Who may do what is decided by the role matrix (E3-3). */
public enum Role {
    ADMIN,
    STAFF,
    READ_ONLY;

    /** Turns the text stored in the users table into a role. An unknown text is refused, never guessed. */
    public static Role fromText(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("A user must have a role");
        }
        return valueOf(text.trim().toUpperCase(Locale.ROOT));
    }
}
