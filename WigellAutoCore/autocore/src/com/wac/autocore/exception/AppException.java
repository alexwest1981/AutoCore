package com.wac.autocore.exception;

/** The parent of the app's own errors, so a caller can catch them in one place. */
public class AppException extends RuntimeException {

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
}
