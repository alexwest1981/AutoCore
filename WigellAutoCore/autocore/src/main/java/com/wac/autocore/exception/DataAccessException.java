package com.wac.autocore.exception;

/** The database did not answer. The SQLException follows as the cause. */
public class DataAccessException extends AppException {

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    /** The reader gets a plain sentence; the message and the SQL cause stay in the log. */
    @Override
    public String getMessageKey() {
        return "view.error.database";
    }
}
