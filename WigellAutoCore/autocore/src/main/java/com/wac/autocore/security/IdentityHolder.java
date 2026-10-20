package com.wac.autocore.security;

import java.util.Optional;

/**
 * Who is using the system right now. The app fills it at login and empties it at logout; the
 * standalone program fills it once at start. Created once by whoever starts the program and handed
 * to whoever needs it, so tests and the two run modes each have their own.
 */
public final class IdentityHolder {
    // volatile, not ThreadLocal: one person is signed in for the whole session, and background
    // threads in JavaFX must see the same identity as the window.
    private  volatile Identity current;

    public void set(Identity identity) {
        if (identity == null) {
            throw new IllegalArgumentException("Use clear() to sign out, not set(null).");
        }
        this.current = identity;
    }

    /** Empty when nobody is signed in, which is how the guard tells "not signed in" from "not allowed". */
    public Optional<Identity> current() {
        return Optional.ofNullable(current);
    }

    public void clear() {
        this.current = null;
    }
}
