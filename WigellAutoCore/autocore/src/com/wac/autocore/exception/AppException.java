package com.wac.autocore.exception;

/**
 * The parent of every error the application raises itself, so a caller can catch our own
 * failures in one place instead of guessing at the type.
 */
public class AppException extends RuntimeException {

    public AppException(String message) {
        super(message);
    }

    public AppException(String message, Throwable cause) {
        super(message, cause);
    }
}
