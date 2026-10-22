package com.wac.autocore.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The parent of the app's own errors, so a caller can catch them in one place.
 *
 * <p>A refusal writes its own line in the log the moment it is raised, not when a window gets
 * around to showing it. That is what makes the log hold every no: the window, the console and the
 * stand-alone program all end up in the same place, and a caller that forgets to log cannot make
 * one disappear. Every one of these in the code is thrown the moment it is made, so it is one line
 * per refusal.
 *
 * <p>Access denied and not signed in stay quiet here: the guard has already written who tried
 * what, and the log does not need it twice.
 */
public class AppException extends RuntimeException {

    private static final Logger log = LoggerFactory.getLogger(AppException.class);

    public AppException(String message) {
        super(message);
    }

    public AppException(String message, Throwable cause) {
        super(message, cause);
    }
    /** The text key the ui translates, or null when the error has no sentence of its own. */
    public String getMessageKey() {
        return null;
    }

    /**
     * Called last in the constructor of every kind that is worth a line, once the fields the key
     * is built from have been set.
     */
    protected final void noteRaised() {
        if (this instanceof DataAccessException || this instanceof DatabaseInitException) {
            // The database is a fault and not a refusal. The message is technical, and the cause
            // is what makes the line useful.
            log.error("Databasfel: {}", getMessage(), this);
            return;
        }
        // The class and the text key say what went wrong. The message may carry a customer's name
        // or number, so it stays out of the log.
        String key = getMessageKey();
        log.warn("Åtgärden avvisades: {}{}", getClass().getSimpleName(), key == null ? "" : " (" + key + ")");
    }
}
