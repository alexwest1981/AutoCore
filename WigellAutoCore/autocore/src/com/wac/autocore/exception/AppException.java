package com.wac.autocore.exception;

/** The parent of the app's own errors, so a caller can catch them in one place. */
public class AppException extends RuntimeException {

    public AppException(String message) {
        super(message);
    }

    public AppException(String message, Throwable cause) {
        super(message, cause);
    }
}
