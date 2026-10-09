package com.wac.autocore.exception;

/** A row the caller asked for is not there. The message names it for the log. */
public class NotFoundException extends AppException {

    public NotFoundException(String message) {
        super(message);
    }
}
