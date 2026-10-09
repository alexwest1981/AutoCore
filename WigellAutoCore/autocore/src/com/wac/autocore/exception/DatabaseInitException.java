package com.wac.autocore.exception;

/** The database could not be prepared, and then nothing else in the app can work. */
public class DatabaseInitException extends AppException {

    public DatabaseInitException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }
}
