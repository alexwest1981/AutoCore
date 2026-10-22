package com.wac.autocore.exception;

/** The database could not be prepared, and then nothing else in the app can work. */
public class DatabaseInitException extends AppException {

    public DatabaseInitException(String message) {
        super(message);
        noteRaised();
    }

    public DatabaseInitException(String message, Throwable cause) {
        super(message);
        initCause(cause);
        noteRaised();
    }

    /**
     * The reader gets one sentence; the SQL cause and the message stay in the log, where the line
     * already carries them.
     */
    @Override
    public String getMessageKey() {
        return "view.error.startup";
    }
}
