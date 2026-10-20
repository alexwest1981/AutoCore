package com.wac.autocore.exception;

/** The database did not answer. The SQLException follows as the cause. */
public class DataAccessException extends AppException {

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
